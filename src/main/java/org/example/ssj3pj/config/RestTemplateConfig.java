package org.example.ssj3pj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    // 업로드용 타임아웃 설정 대용량 업로드시 청크당 응답 속도 저하 고려
    @Bean(name = "youtubeRestTemplate")
    public RestTemplate youtubeRestTemplate(
            @Value("${youtube.upload.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${youtube.upload.read-timeout-ms:600000}") int readTimeoutMs
    ) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connectTimeoutMs);
        f.setReadTimeout(readTimeoutMs);
        return new RestTemplate(f);
    }
}