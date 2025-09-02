package org.example.ssj3pj.config.runner;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3TestRunner {

    private final S3Client s3Client;

    @PostConstruct
    public void testS3Connection() {
        try {
            ListBucketsResponse response = s3Client.listBuckets();
            log.info("✅ S3 연결 성공! 버킷 목록:");
            response.buckets().forEach(b -> log.info(" - {}", b.name()));
        } catch (Exception e) {
            log.error("❌ S3 연결 실패: {}", e.getMessage(), e);
        }
    }
}
