package org.example.ssj3pj.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sts.StsClient;

// AwsConfig.java
@Configuration
public class AwsConfig {
    @Value("${aws.region:us-east-2}") String region;

    @Bean
    public AwsCredentialsProvider awsCreds(
            @Value("${aws.profile:myapp-s3}") String profileName // ← 원하는 프로필명
    ) {
        // 먼저 지정된 프로필을 시도하고, 실패시 기본 자격증명 체인 사용
        try {
            return ProfileCredentialsProvider.builder()
                    .profileName(profileName)
                    .build();
        } catch (Exception e) {
            // 프로필 로드 실패 시 기본 자격증명 체인 사용
            return DefaultCredentialsProvider.create();
        }
    }

    @Bean
    public Region awsRegion() { return Region.of(region); }

    @Bean
    public S3Presigner s3Presigner(Region r, AwsCredentialsProvider p) {
        return S3Presigner.builder().region(r).credentialsProvider(p).build();
    }

    @Bean
    public S3Client s3Client(Region r, AwsCredentialsProvider p) {
        return S3Client.builder().region(r).credentialsProvider(p).build();
    }

    @Bean
    public StsClient stsClient(Region r, AwsCredentialsProvider p) {
        return StsClient.builder().region(r).credentialsProvider(p).build();
    }
}