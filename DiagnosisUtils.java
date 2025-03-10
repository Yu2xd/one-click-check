import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import com.github.s7connector.impl.serializer.converter.RealConverter;


public class DiagnosisUtils {

    /**
     * 获取皮带速度信息
     * 读取速度反馈、频率反馈和给定频率的值，每个值读取5次（每秒读取一次）
     *
     * @return 格式化的速度和频率信息
     * @throws IOException 如果读取失败
     */
    // PLC IP 地址
    private static final String PLC_IP = "192.168.10.22";
    // S7 端口
    private static final int S7_PORT = 102; 
    public static String getBeltSpeedInfo() throws IOException {
        S7Connector s7Connector = null;
        try {
            // 创建S7的连接
            s7Connector = S7ConnectorFactory
                    .buildTCPConnector()
                    .withHost(PLC_IP)//plc Ip地址
                    .withPort(S7_PORT)  // 标准S7端口
                    .withTimeout(10000)  // 连接超时时间
                    .withRack(0)  // 架机号
                    .withSlot(1)  // 插槽号
                    .build();

            // 存储读取的值
            float[] speedFeedbackValues = new float[5];
            float[] frequencyFeedbackValues = new float[5];
            float[] frequencySetpointValues = new float[5];

            // 定义DB4地址和偏移量
            final int dbNumber = 4;  // DB4
            final int speedFeedbackOffset = 28;  // 速度反馈 DB4 28.0
            final int frequencyFeedbackOffset = 20;  // 频率反馈 DB4 20.0
            final int frequencySetpointOffset = 44;  // 频率给定值 DB4 44.0

            // 创建实数转换器
            RealConverter realConverter = new RealConverter();

            // 每秒读取一次，共读取5次
            for (int i = 0; i < 5; i++) {
                // 读取速度反馈值
                byte[] speedData = s7Connector.read(DaveArea.DB, dbNumber, 4, speedFeedbackOffset);
                speedFeedbackValues[i] = realConverter.extract(Float.class, speedData, 0, 0);

                // 读取频率反馈值
                byte[] freqFeedbackData = s7Connector.read(DaveArea.DB, dbNumber, 4, frequencyFeedbackOffset);
                frequencyFeedbackValues[i] = realConverter.extract(Float.class, freqFeedbackData, 0, 0);

                // 读取频率设定值
                byte[] freqSetpointData = s7Connector.read(DaveArea.DB, dbNumber, 4, frequencySetpointOffset);
                frequencySetpointValues[i] = realConverter.extract(Float.class, freqSetpointData, 0, 0);

                // 等待1秒
                if (i < 4) {
                    Thread.sleep(1000);
                }
            }

            // 格式化数据
            StringBuilder result = new StringBuilder();
            result.append("\t 实时速度反馈：").append(formatArray(speedFeedbackValues)).append("\n");
            result.append("\t 实时频率反馈：").append(formatArray(frequencyFeedbackValues)).append("\n");
            result.append("\t 预设频率：").append(formatArray(frequencySetpointValues)).append("\n");

            return result.toString();
        } catch (Exception e) {
            throw new IOException("读取PLC数据失败: " + e.getMessage(), e);
        } finally {
            // 确保连接关闭
            if (s7Connector != null) {
                try {
                    s7Connector.close();
                } catch (Exception e) {
                    // 关闭连接时的异常可以记录但不要抛出
                    System.err.println("关闭PLC连接失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 将浮点数组格式化为 [x.xx,x.xx,x.xx,x.xx,x.xx] 形式的字符串
     *
     * @param array 浮点数组
     * @return 格式化的字符串
     */
    private static String formatArray(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            // 使用String.format保留两位小数
            sb.append(String.format("%.2f", array[i]));
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取指定文件的最后修改时间
     *
     * @param filePath 文件路径
     * @return 文件的最后修改时间（LocalDateTime格式）
     * @throws IOException 如果文件不存在或发生I/O错误
     */
    public static String getFileLastModifiedTime(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        LocalDateTime date =  Files.getLastModifiedTime(path)
                                    .toInstant() // 转换为时间戳
                                    .atZone(ZoneId.systemDefault()) // 转换为系统时区
                                    .toLocalDateTime(); // 转换为本地日期时间
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return date.format(customFormatter);
    }

    public static String getFilesListLastModifiedTime(List<String> filesList) throws IOException {
        String res = new String();
        for (String file : filesList) {
            res = res + "\t" + String.format("%s : %s",file, getFileLastModifiedTime(file)) + "\n"; 
        }
        return res;
    }

    /**
     * 获取日期最新的 2 张 PNG 图片路径
     * 
     * @param startPath 检索目录
     * @return List<path> 最新的 2 张 PNG 图片路径
     * @throws IOException
     */
    public static List<Path> findLatestTwoBlank(Path startPath) throws IOException {
        // 收集所有符合条件的PNG文件
        Stream<Path> pngCandidates = Files.walk(startPath)
                .filter(Files::isDirectory) // 只处理目录
                .filter(path -> path.getFileName().toString().equals("blank")) // 筛选blank文件夹
                .flatMap(blankDir -> {
                    Path highDir = blankDir.resolve("High");
                    if (Files.exists(highDir) && Files.isDirectory(highDir)) { // 存在High目录
                        // 仅遍历High目录的直接文件（不递归子目录）
                        try {
                            return Files.list(highDir) // 使用list()获取当前目录内容
                                    .filter(Files::isRegularFile) // 只处理文件
                                    .filter(p -> p.getFileName().toString()
                                            .toLowerCase()
                                            .endsWith(".png")); // 不区分大小写的扩展名匹配
                        } catch (IOException e) {
                            throw new RuntimeException("读取High目录失败", e);
                        }
                    } else {
                        return Stream.empty(); // 返回空Stream
                    }
                });
        
        List<Path> resList = new ArrayList<>();
        List<Path> pngCandidatesList = pngCandidates.collect(Collectors.toList());
        // 找出最新的PNG文件
        if (!pngCandidatesList.isEmpty()) {
            // 排序规则：按修改时间降序，时间相同按路径逆序
            List<Path> sortedList = pngCandidatesList.stream()
            .sorted(Comparator.comparing(path -> {
                try {
                    return Files.getLastModifiedTime(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            )
            .collect(Collectors.toList());

            Integer lenList = sortedList.size();
            
            if (lenList >= 2) {
                resList.add(sortedList.get(lenList - 1));
                resList.add(sortedList.get(lenList - 2));
            }
        }
        
        return resList; 
    }

    /**
     * 获取当前目录下最新的 2 张空皮带，并检测是否存在异常
     * 
     * @param startPath 存放数据的目录路径
     * @return String ,res 空皮带检测结果数据
     * @throws IOException
     */
    public static String checkBlankImg(String startPath) throws IOException {
        String res = new String();
        Path path = Paths.get(startPath);
        // 获取日期最新的 2 张 PNG 图片路径
        List<Path> twoBlankPath = findLatestTwoBlank(path);
        if (twoBlankPath.size() == 2) {
            // 皮带异常检测
            List<Object> resList = blankAnomalyDetection(twoBlankPath.get(0), twoBlankPath.get(1));
            if (resList.size() == 3) {
                res = String.format("mean_std：%s, max_std：%s, min_std: %s",
                resList.get(0), resList.get(1), resList.get(2));
            }
        } else {
            return new String("底噪图片不足 2 张");
        }
        
            return res;
    }

    /**
     * 
     * @param lastOnePath 最新的 blank 图片路径
     * @param lastSecondPath 次新的 blank 图片路径
     * @return List: [平均标准差，最大标准差，最小标准差]
     * @throws IOException
     */
    public static List<Object> blankAnomalyDetection(Path lastOnePath, Path lastSecondPath) throws IOException {

        String pyExe = "python ./BlankImgCheck.py";
        String pythonCmd = String.format("%s %s %s", pyExe, lastOnePath, lastSecondPath);
        String resString = pythonExecution(pythonCmd);
        List<Object> resList = extractInfo(resString);
        return resList;
    }


    /**
     * 执行命令行命令 pythonCmd
     * 
     * @param pythonCmd 执行python的命令行命令
     * @return string, python 程序的输出结果
     */
    public static String pythonExecution(String pythonCmd) {
        // 定义缓冲区、正常结果输出流、错误信息输出流
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream outerrStream = new ByteArrayOutputStream();
        try {
            Process proc;

            proc = Runtime.getRuntime().exec(pythonCmd);

            InputStream errStream = proc.getErrorStream();
            InputStream stream = proc.getInputStream();

            // 流读取与写入
            int len = -1;
            while ((len = errStream.read(buffer)) != -1) {
                outerrStream.write(buffer, 0, len);
            }

            while ((len = stream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            proc.waitFor();// 等待命令执行完成

            // 打印流信息
            if (!outStream.toString().isEmpty()) {
                // System.out.println(outStream.toString());
                return outStream.toString();
            }
            if (!outerrStream.toString().isEmpty()) {
                // System.out.println(outerrStream.toString());
                return outStream.toString();
            }
            proc.waitFor();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new String();

    }

    /**
     * 从 python 程序的输出结果中提取出统计信息
     * 
     * @param resString , python 程序输出的字符串
     * @return list, 提取出来的统计信息。若出错，则为空
     */
    private static List<Object> extractInfo(String resString){
        List<Object> resList = new ArrayList<>();
        if (resString.contains("success")) {
            // 正则表达式：匹配整数或小数（不含前导/后导小数点）
            String regex = "\\d+(\\.\\d+)?|\\.\\d+";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(resString);

            while (matcher.find()) {
                String num = matcher.group();
                // 可选：过滤无效格式（如 "123."）
                if (num.matches("\\d+\\.?\\d*")) {
                    resList.add(num);
                }
            }
        }
        return resList;
    }


    public static final String DEFAULT_VALUE = "未知";

    private static String StringEmptyCheck(String str) {
        return (str == null || str.isEmpty()) ? DEFAULT_VALUE : str;
    }
    /**
     * 根据输入信息，生成检测报告 txt 文件
     * 
     * @param xRayState 光机状态
     * @param detectorState 探测器状态
     * @param penState 喷吹状态
     * @param beltSpeedState 皮带速度状态
     * @param progState 分选程序状态
     * @param collectState 采集程序状态
     * @throws IOException
     */
    public static String generateCheckReport(String xRayState, String detectorState, String penState, 
                                        String beltSpeedState, String progState, String collectState,
                                        String curProgAndModelReplaceTime, String blankDetectionState) throws IOException {
        // 非空检验
        String finalXRayState = StringEmptyCheck(xRayState);
        String finalDetectorState = StringEmptyCheck(detectorState);
        String finalPenState = StringEmptyCheck(penState);
        String finalBeltSpeedState = StringEmptyCheck(beltSpeedState);
        String finalProgState = StringEmptyCheck(progState);
        String finalCollectState = StringEmptyCheck(collectState);
        String finalCurProgAndModelReplaceTime = StringEmptyCheck(curProgAndModelReplaceTime);
        String finalBlankDetectionState = StringEmptyCheck(blankDetectionState);

        // 生成时间戳
        Date date =new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String checkReportName = String.format("checkReport_%s.txt", sdf.format(date));

        try{
            FileWriter reportFileWriter = new FileWriter(checkReportName);
            BufferedWriter bw = new BufferedWriter(reportFileWriter);
            bw.write(">>光机：" + finalXRayState);
            bw.newLine();
            bw.newLine();
            bw.write(">>探测器：" + finalDetectorState);
            bw.newLine();
            bw.newLine();
            bw.write(">>喷吹：" + finalPenState);
            bw.newLine();
            bw.newLine();
            bw.write(">>皮带速度：\n" + finalBeltSpeedState);
            bw.newLine();
            bw.newLine();
            bw.write(">>底噪检测：" + finalBlankDetectionState);
            bw.newLine();
            bw.newLine();
            bw.write(">>分选程序：" + finalProgState);
            bw.newLine();
            bw.newLine();
            bw.write(">>采集数据：" + finalCollectState);
            bw.newLine();
            bw.newLine();
            bw.write(">>当前程序/模型的替换时间：\n" + finalCurProgAndModelReplaceTime);
            
            bw.close();
            reportFileWriter.close();
            return checkReportName;
        } catch (IOException e) {
            e.printStackTrace();
            return new String();
        }
        
    }

    public static void testGetFileLastModifiedTime() {
        try {
            String filePath = "D:/2023SummerWork/点检系统/一键诊断/诊断报告(第一版).txt"; // 替换为实际文件路径
            String lastModified = getFileLastModifiedTime(filePath);
            System.out.println("文件最后修改时间: " + lastModified);
        } catch (IOException e) {
            System.err.println("无法读取文件时间: " + e.getMessage());
        }
    }

    public static void testCheckBlankImg() {
        String startPath = "D:/2023SummerWork/点检系统/一键诊断";
        try {
            String res = checkBlankImg(startPath);
            System.out.println(res);
        } catch (IOException e) {
            System.err.println("处理失败: " + e.getMessage());
        }
    }

    public static void testFindLatestTwoBlank() {
        Path startPath = Paths.get("D:/2023SummerWork/点检系统/一键诊断");
        try {
            List<Path> latestPng = findLatestTwoBlank(startPath);
            System.out.println("最新PNG文件路径: " + latestPng);
        } catch (IOException e) {
            System.err.println("处理失败: " + e.getMessage());
        }
    }

    public static void testGenerateCheckReport() {
        try {
            generateCheckReport("",null,"","","","",null, null);
            System.out.println("检测报告生成完成");
        } catch (IOException e) {
            System.err.println("检测报告生成失败: " + e.getMessage());
        }
    }

    

    public static void main(String[] args) {
        // 调用实例

        // 获取文件的最新修改日期
        //testGetFileLastModifiedTime();

        // 获取最新的两张 Blank 图像路径
        //testFindLatestTwoBlank();

        // 皮带异常检测
        //testCheckBlankImg();

        // 生成检测报告
        //testGenerateCheckReport();
    }
}