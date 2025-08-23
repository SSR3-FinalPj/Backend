// src/main/java/org/example/ssj3pj/config/SseConfig.java
package org.example.ssj3pj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SseConfig {

    @Bean
    public SseSettings sseSettings(
            @Value("${sse.reconnect-millis:3000}") long reconnectMillis,
            @Value("${sse.auth.query-param:sse_token}") String tokenQueryParam,
            @Value("${sse.dev.allow-userid-param:false}") boolean devAllowUserIdParam
    ) {
        return new SseSettings(reconnectMillis, tokenQueryParam, devAllowUserIdParam);
    }

    public record SseSettings(long reconnectMillis, String tokenQueryParam, boolean devAllowUserIdParam) {}
}
