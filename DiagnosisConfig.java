package org.example.controlsys.demos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * DiagnosisConfig - 诊断系统配置类
 * 单例模式实现，确保配置只加载一次并在整个应用生命周期内可用
 */
public class DiagnosisConfig {
    private static final Logger logger = Logger.getLogger(DiagnosisConfig.class.getName());

    // 单例实例
    private static DiagnosisConfig instance;

    // 配置API地址
    private static final String CONFIG_API_URL = "http://your-config-api-endpoint";

    // MinIO配置
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String objectPath;
    private String localFilePath;

    // PLC配置
    private String plcIp;
    private int s7Port;
    private int rack;
    private int slot;
    private int dbNumber;
    private int speedFeedbackOffset;
    private int frequencyFeedbackOffset;
    private int frequencySetpointOffset;

    // 文件配置 - 保持原来的硬编码方式
    private final List<String> filesName = Arrays.asList(
            "D:\\idea projects\\controlsys\\src\\main\\java\\org\\example\\controlsys\\demos\\controller\\BlankImgCheck.py",
            "D:\\idea projects\\controlsys\\src\\main\\java\\org\\example\\controlsys\\demos\\controller\\DiagnosisImpl.java",
            "D:\\idea projects\\controlsys\\src\\main\\java\\org\\example\\controlsys\\demos\\controller\\DiagnosisUtils.java"
    );

    private final String dataDir = "";

    // 私有构造函数防止外部实例化
    private DiagnosisConfig() {
        // 初始化默认值
        endpoint = "http://221.2.171.221:34242";
        accessKey = "jzth";
        secretKey = "JzTh267421";
        bucketName = "uploadtest";
        objectPath = "checkReport/";
        localFilePath = "./";

        plcIp = "192.168.10.22";
        s7Port = 102;
        rack = 0;
        slot = 1;
        dbNumber = 4;
        speedFeedbackOffset = 28;
        frequencyFeedbackOffset = 20;
        frequencySetpointOffset = 44;
    }

    /**
     * 获取单例实例
     * @return DiagnosisConfig实例
     */
    public static synchronized DiagnosisConfig getInstance() {
        if (instance == null) {
            instance = new DiagnosisConfig();
            instance.loadConfigFromApi();
        }
        return instance;
    }

    /**
     * 从配置API加载配置
     * 仅在首次访问时调用一次
     */
    private void loadConfigFromApi() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(CONFIG_API_URL, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject config = JSONObject.parseObject(response.getBody());

                // 更新MinIO配置
                if (config.containsKey("minio")) {
                    JSONObject minioConfig = config.getJSONObject("minio");
                    this.endpoint = minioConfig.getString("endpoint");
                    this.accessKey = minioConfig.getString("accessKey");
                    this.secretKey = minioConfig.getString("secretKey");
                    this.bucketName = minioConfig.getString("bucketName");
                    this.objectPath = minioConfig.getString("objectPath");
                    this.localFilePath = minioConfig.getString("localFilePath");
                }

                // 更新PLC配置
                if (config.containsKey("plc")) {
                    JSONObject plcConfig = config.getJSONObject("plc");
                    this.plcIp = plcConfig.getString("ip");
                    this.s7Port = plcConfig.getIntValue("port");
                    this.rack = plcConfig.getIntValue("rack");
                    this.slot = plcConfig.getIntValue("slot");
                    this.dbNumber = plcConfig.getIntValue("dbNumber");
                    this.speedFeedbackOffset = plcConfig.getIntValue("speedFeedbackOffset");
                    this.frequencyFeedbackOffset = plcConfig.getIntValue("frequencyFeedbackOffset");
                    this.frequencySetpointOffset = plcConfig.getIntValue("frequencySetpointOffset");
                }

                // 不再从API加载filesName和dataDir配置

                logger.info("Configuration loaded successfully from API");
            } else {
                logger.warning("Failed to load configuration from API, using default values");
            }
        } catch (Exception e) {
            logger.warning("Error loading configuration from API: " + e.getMessage() + ". Using default values.");
        }
    }

    /**
     * 获取PLC配置对象
     * @return 包含所有PLC参数的配置对象
     */
    public DiagnosisUtils.PlcConfig getPlcConfig() {
        return new DiagnosisUtils.PlcConfig(
                plcIp,
                s7Port,
                rack,
                slot,
                dbNumber,
                speedFeedbackOffset,
                frequencyFeedbackOffset,
                frequencySetpointOffset
        );
    }

    /**
     * 强制刷新配置（如有需要）
     */
    public void refreshConfig() {
        loadConfigFromApi();
    }

    // Getters for all configuration properties

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public String getPlcIp() {
        return plcIp;
    }

    public int getS7Port() {
        return s7Port;
    }

    public int getRack() {
        return rack;
    }

    public int getSlot() {
        return slot;
    }

    public int getDbNumber() {
        return dbNumber;
    }

    public int getSpeedFeedbackOffset() {
        return speedFeedbackOffset;
    }

    public int getFrequencyFeedbackOffset() {
        return frequencyFeedbackOffset;
    }

    public int getFrequencySetpointOffset() {
        return frequencySetpointOffset;
    }

    public List<String> getFilesName() {
        return filesName;
    }

    public String getDataDir() {
        return dataDir;
    }
}