package org.example.ssj3pj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sts.StsClient;

@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-2}")
    private String region;

    /**
     * AWS SDK 기본 자격 증명 체인 사용
     * 순서: 환경변수(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) → 시스템 속성 → 프로필(~/.aws/credentials) → EC2/ECS/EKS 메타데이터
     */
    @Bean
    public AwsCredentialsProvider awsCreds() {
        AwsCredentialsProvider provider = DefaultCredentialsProvider.create();
        return provider;
    }

    @Bean
    public Region awsRegion() {
        return Region.of(region);
    }

    @Bean
    public S3Presigner s3Presigner(Region r, AwsCredentialsProvider p) {
        return S3Presigner.builder()
                .region(r)
                .credentialsProvider(p)
                .build();
    }

    @Bean
    public S3Client s3Client(Region r, AwsCredentialsProvider p) {
        return S3Client.builder()
                .region(r)
                .credentialsProvider(p)
                .build();
    }

    @Bean
    public StsClient stsClient(Region r, AwsCredentialsProvider p) {
        return StsClient.builder()
                .region(r)
                .credentialsProvider(p)
                .build();
    }
}
