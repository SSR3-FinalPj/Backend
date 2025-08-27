package org.example.ssj3pj.services.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OAuthStateService {

    private final StringRedisTemplate redis;

    @Value("${oauth.state.ttl-seconds:600}")
    private long defaultTtlSeconds;

    private static final SecureRandom RAND = new SecureRandom();

    private static String randomNonce() {
        byte[] buf = new byte[32]; // 256-bit
        RAND.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String key(String provider, String nonce) {
        return "oauth:state:" + provider + ":" + nonce;
    }

    /** 공통 발급: provider 예) "google", "reddit" */
    public String issueState(String provider, Long userId) {
        Objects.requireNonNull(provider, "provider");
        String nonce = randomNonce();
        redis.opsForValue().set(key(provider, nonce), String.valueOf(userId), Duration.ofSeconds(defaultTtlSeconds));
        return nonce;
    }

    /** 공통 소비 */
    public Long consumeState(String provider, String nonce) {
        Objects.requireNonNull(provider, "provider");
        String k = key(provider, nonce);
        String val = redis.opsForValue().get(k);
        if (val == null) return null;   // 만료/위조/재사용
        redis.delete(k);                // 1회성 소비
        try {
            return Long.valueOf(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
