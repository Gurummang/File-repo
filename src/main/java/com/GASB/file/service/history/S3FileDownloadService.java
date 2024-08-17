package com.GASB.file.service.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

@Service
public class S3FileDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(S3FileDownloadService.class);
    private final S3Client s3Client;

    public S3FileDownloadService(S3Client s3Client){
        this.s3Client = s3Client;
    }

    /**
     * S3에서 파일을 스트리밍 방식으로 다운로드합니다.
     *
     * @param bucketName    S3 버킷 이름
     * @param key           S3 객체의 키 (경로)
     * @return InputStream  S3에서 다운로드한 파일의 InputStream
     */

    public InputStream downloadFile(String bucketName, String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            InputStream inputStream = s3Client.getObject(getObjectRequest);
            logger.info("File streaming started for bucket: {}, key: {}", bucketName, key);
            return inputStream;

        } catch (Exception e) {
            logger.error("Failed to download file from S3. Bucket: {}, Key: {}, Error: {}",
                    bucketName, key, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }
}
