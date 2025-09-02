package org.example.ssj3pj.services;
import lombok.extern.slf4j.Slf4j;   // ✅ 이거 추가

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    @Value("${aws.region:us-east-2}") private String awsRegion;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.presign.ttl-minutes:15}")
    private long ttlMinutes;

    /** 업로드용 Presigned PUT URL */
    public String presignPut(String key, String contentType) {

        // ✅ presign 직전 “누가 서명하는지” 로그
        try (StsClient sts = StsClient.builder()
                .region(Region.of(awsRegion)) // S3 리전과 맞추기
                .build()) {
            GetCallerIdentityResponse me = sts.getCallerIdentity();
            log.info("[S3 PRESIGN] principal arn={}, account={}, userId={}",
                    me.arn(), me.account(), me.userId());
        } catch (Exception e) {
            log.warn("[S3 PRESIGN] STS GetCallerIdentity 실패: {}", e.toString());
        }
        var put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        var pre = PutObjectPresignRequest.builder()
                .putObjectRequest(put)
                .signatureDuration(Duration.ofMinutes(ttlMinutes))
                .build();

        return presigner.presignPutObject(pre).url().toString();
    }

    /** 다운로드/재생용 Presigned GET URL */
    public String presignGet(String key, String responseContentType) {
        var get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentType(responseContentType)
                .responseContentDisposition("inline")
                .build();
        var pre = GetObjectPresignRequest.builder()
                .getObjectRequest(get)
                .signatureDuration(Duration.ofMinutes(ttlMinutes))
                .build();
        return presigner.presignGetObject(pre).url().toString();
    }

    public String presignDownload(String key) {
        var get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentType("application/octet-stream")
                .responseContentDisposition("attachment; filename=\""
                        + Paths.get(key).getFileName().toString() + "\"")
                .build();

        var pre = GetObjectPresignRequest.builder()
                .getObjectRequest(get)
                .signatureDuration(Duration.ofMinutes(ttlMinutes))
                .build();

        return presigner.presignGetObject(pre).url().toString();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "anon" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** ← 추가: 업로드 확인용 HEAD */
    public HeadObjectResponse head(String key) {
        return s3.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    /** S3에서 임시 파일로 다운로드 (YouTube 업로드용) */
    public Path downloadToTemporary(String resultKey) throws IOException {
        // 임시 파일 경로 생성 (확장자 추출해서 사용)
        String extension = getFileExtension(resultKey);
        Path tempFile = Files.createTempFile("youtube-upload-", extension);

        // 충돌 방지를 위해 먼저 생성된 빈 파일 삭제
        Files.deleteIfExists(tempFile);

        log.info("S3에서 임시 파일로 다운로드 시작: {} -> {}", resultKey, tempFile);
        
        try {
            // S3Client로 직접 다운로드 (Presigned URL 문제 우회)
            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest =
                software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(resultKey)
                    .build();

            // S3에서 파일을 직접 임시 파일에 다운로드
            software.amazon.awssdk.services.s3.model.GetObjectResponse response =
                s3.getObject(getObjectRequest, tempFile);

            long fileSize = Files.size(tempFile);
            log.info("S3 파일 다운로드 완료: {} bytes", fileSize);
            return tempFile;
            
        } catch (Exception e) {
            // 임시 파일 정리
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            
            throw new IOException("S3 파일 다운로드 실패: " + resultKey, e);
        }
    }

    /** 임시 파일 정리 */
    public void cleanupTemporaryFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
                log.info("임시 파일 삭제 완료: {}", tempFile);
            } catch (IOException e) {
                log.warn("임시 파일 삭제 실패: {}", tempFile, e);
            }
        }
    }

    /** 파일 확장자 추출 */
    private String getFileExtension(String key) {
        int lastDot = key.lastIndexOf('.');
        if (lastDot > 0 && lastDot < key.length() - 1) {
            return key.substring(lastDot);
        }
        return ".mp4"; // 기본값
    }
}
