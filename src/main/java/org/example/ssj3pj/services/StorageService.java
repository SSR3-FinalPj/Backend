package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.presign.ttl-minutes:15}")
    private long ttlMinutes;

    /** 비디오 키 규칙 */
    public String videoKey(String videoId) {
        return "videos/" + videoId + "/master.mp4";
    }

    /** 업로드용 Presigned PUT URL */
    public String presignPut(String key, String contentType) {
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
    public String newImageKey(String userId, String ext) {
        LocalDate d = LocalDate.now();
        return String.format(
                "images/%s/%04d/%02d/%02d/%s.%s",
                safe(userId), d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                UUID.randomUUID(), ext.toLowerCase(Locale.ROOT)
        );
    }
    private String safe(String s) {
        return (s == null || s.isBlank()) ? "anon" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
