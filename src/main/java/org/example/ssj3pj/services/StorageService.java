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

import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
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

    /** 비디오 키 규칙 */
    public String videoKey(String videoId) {
        return "videos/" + "/master.mp4";
    }

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
}
