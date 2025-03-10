
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import com.alibaba.fastjson.JSONObject;

public class DiagnosisImpl {

    // 需要查看最新修改日期的文件
    public static final List<String> filesName = Arrays.asList("./BlankImgCheck.py", 
                                                                "./DiagnosisImpl.java",
                                                                "./DiagnosisUtils.java"); 

    public static final String  endpoint = "http://221.2.171.221:34242";//minio服务器地址

    public static final String  accessKey = "jzth";//minio服务器用户名

    public static final String  secretKey = "JzTh267421";//minio服务器密码

    public static final String  bucketName = "uploadtest";//minio服务器桶名

    public static final String  objectPath = "document/asd/";//minio服务器桶内路径

    public static final String  localFilePath = "./";//本地文件路径

    // 数据集存放路径
    public static final String  dataDir = "";

    public static void checkReport() throws IOException {   
        // TODO 获取光机信息, 获取失败请返回空字符串
        String xRayState = "";

        // TODO 获取探测器信息, 获取失败请返回空字符串
        String detectorState = "";

        // TODO 获取喷吹信息, 获取失败请返回空字符串
        String penState = "";

        // TODO 获取分选程序信息, 获取失败请返回空字符串
        String progState = "";

        // TODO 获取采集数据信息, 获取失败请返回空字符串
        String collectState = "";

        // TODO 获取皮带速度信息, 获取失败请返回空字符串
        String beltSpeedState = "";
        try {
            // 调用新实现的方法获取皮带速度信息
            beltSpeedState = DiagnosisUtils.getBeltSpeedInfo();
        } catch (IOException e) {
            System.err.println("获取皮带速度信息失败: " + e.getMessage());
            // 如果获取失败，保持空字符串
            beltSpeedState = "";
        }

        // 获取当前程序/模型的替换时间
        String curProgAndModelReplaceTime = DiagnosisUtils.getFilesListLastModifiedTime(filesName);

        // 皮带异常检测结果
        String blankDetectionState = DiagnosisUtils.checkBlankImg(dataDir);

        // 生成检测报告
        String checkReportName = DiagnosisUtils.generateCheckReport(xRayState, detectorState, penState, beltSpeedState, progState, 
                                            collectState, curProgAndModelReplaceTime, blankDetectionState);

        System.out.println(checkReportName);
        // 返回信息
        // JSONObject res = new JSONObject();
        if (!checkReportName.isEmpty()) {
            System.out.println(String.format("开始上传: %s", checkReportName));
            // 上传检测报告
            MinioUploader uploader = new MinioUploader(endpoint, accessKey, secretKey);
            try {
                // 上传文件
                uploader.uploadFile(
                        bucketName,                      // 存储桶名称
                        objectPath + checkReportName,          // 对象存储路径
                        localFilePath + checkReportName // 本地文件路径
                );
                System.out.println("File uploaded successfully");
                // 返回成功信息
                // res.put("code", 200)
            } catch (Exception e) {
                System.err.println("Error occurred: " + e.getMessage());
                e.printStackTrace();
                // 返回失败信息
                // res.put("code", 500)
            }
        } 

        // TODO 响应请求
        // 1、checkReport 返回类型改为 JSONObject
        // 2、用法可查看代码 line 59、73、78、85
        // return res
    }

    public static void main(String[] args) {
        try {
            checkReport();
        } catch (IOException e) {
            System.err.println("处理失败: " + e.getMessage());
        }
    }
}
