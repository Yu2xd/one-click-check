import io.minio.*;
import io.minio.errors.MinioException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MinioUploader {
    private final MinioClient minioClient;

    /**
     * 初始化MinIO客户端
     * @param endpoint   MinIO服务地址（包含端口）
     * @param accessKey  访问密钥
     * @param secretKey  私有密钥
     */
    public MinioUploader(String endpoint, String accessKey, String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 上传文件到MinIO
     * @param bucketName     存储桶名称
     * @param objectPath    对象存储路径（不含存储桶）
     * @param localFilePath 本地文件完整路径
     * @throws Exception 包含各种可能的异常
     */
    public void uploadFile(String bucketName, String objectPath, String localFilePath) throws Exception {
        // 验证参数有效性
        validateParameters(bucketName, objectPath);

        // 处理本地文件
        File file = validateLocalFile(localFilePath);

        // 自动推断内容类型
        String contentType = detectContentType(file);

        // 执行文件上传
        try (InputStream fileStream = new FileInputStream(file)) {
            // 确保存储桶存在
            ensureBucketExists(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(fileStream, file.length(), -1)
                            .contentType(contentType)
                            .build());
        }
    }

    private void validateParameters(String bucketName, String objectPath) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name cannot be empty");
        }
        if (objectPath == null || objectPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Object path cannot be empty");
        }
    }

    private File validateLocalFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Local file not found: " + filePath);
        }
        if (!file.isFile()) {
            throw new IOException("Path is not a file: " + filePath);
        }
        return file;
    }

    private String detectContentType(File file) throws IOException {
        String contentType = Files.probeContentType(file.toPath());
        return contentType != null ? contentType : "application/octet-stream";
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }
}