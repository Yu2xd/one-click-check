package org.example.controlsys.demos.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import com.alibaba.fastjson.JSONObject;

import static org.example.controlsys.demos.controller.DiagnosisUtils.getLatestRecords;

public class DiagnosisImpl {

    public static JSONObject checkReport() throws IOException {
        // 获取配置单例实例
        DiagnosisConfig config = DiagnosisConfig.getInstance();
        // TODO 获取光机信息, 获取失败请返回空字符串
        String xRayState = "";

        // TODO 获取探测器信息, 获取失败请返回空字符串
        String detectorState = "";

        // TODO 获取喷吹信息, 获取失败请返回空字符串
        String penState =getLatestRecords(3,"D:\\idea projects\\controlsys\\src\\main\\java\\org\\example\\controlsys\\demos\\controller\\jet.txt");

        // TODO 获取分选程序信息, 获取失败请返回空字符串
        String progState = "";

        // TODO 获取采集数据信息, 获取失败请返回空字符串
        String collectState = "";

        // TODO 获取皮带速度信息, 获取失败请返回空字符串
        String beltSpeedState = "";
        try {
            // 创建PLC配置对象并传入getBeltSpeedInfo方法
            DiagnosisUtils.PlcConfig plcConfig = config.getPlcConfig();
            beltSpeedState = DiagnosisUtils.getBeltSpeedInfo(plcConfig);
        } catch (IOException e) {
            System.err.println("获取皮带速度信息失败: " + e.getMessage());
            // 如果获取失败，保持空字符串
            beltSpeedState = "";
        }

        // 获取当前程序/模型的替换时间
        String curProgAndModelReplaceTime = DiagnosisUtils.getFilesListLastModifiedTime(config.getFilesName());

        // 皮带异常检测结果
        String blankDetectionState = DiagnosisUtils.checkBlankImg(config.getDataDir());

        // 生成检测报告
        String checkReportName = DiagnosisUtils.generateCheckReport(xRayState, detectorState, penState, beltSpeedState, progState,
                collectState, curProgAndModelReplaceTime, blankDetectionState);

        System.out.println(checkReportName);
        // 返回信息
        JSONObject res = new JSONObject();
        if (!checkReportName.isEmpty()) {
            System.out.println(String.format("开始上传: %s", checkReportName));
            // 上传检测报告
            MinioUploader uploader = new MinioUploader(
                    config.getEndpoint(),
                    config.getAccessKey(),
                    config.getSecretKey()
            );
            try {
                // 上传文件
                uploader.uploadFile(
                        config.getBucketName(),                      // 存储桶名称
                        config.getObjectPath() + checkReportName,    // 对象存储路径
                        config.getLocalFilePath() + checkReportName  // 本地文件路径
                );
                System.out.println("File uploaded successfully");
                //返回成功信息
                res.put("code", 200);
            } catch (Exception e) {
                System.err.println("Error occurred: " + e.getMessage());
                e.printStackTrace();
                //返回失败信息
                res.put("code", 500);
            }
        }

        // TODO 响应请求
        // 1、checkReport 返回类型改为 JSONObject
        // 2、用法可查看代码 line 59、73、78、85
        return res;
    }

    public static void main(String[] args) {
        try {
            checkReport();
//            DiagnosisUtils.getAllDeviceInfo();
        } catch (IOException e) {
            System.err.println("处理失败: " + e.getMessage());
        }
    }
}