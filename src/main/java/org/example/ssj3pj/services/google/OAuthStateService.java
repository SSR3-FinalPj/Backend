package org.example.ssj3pj.services.google;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

// 구글 연동 state 변수로 보안
@Service
@RequiredArgsConstructor
public class OAuthStateService {

    private final StringRedisTemplate redis;

    @Value("${oauth.state.ttl-seconds:600}")
    private long ttlSeconds;

    private static final SecureRandom RAND = new SecureRandom();

    private static String randomNonce() {
        byte[] buf = new byte[32]; // 256-bit
        RAND.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String key(String nonce) { return "oauth:state:" + nonce; }

    public String issueState(Long userId) {
        String nonce = randomNonce();
        redis.opsForValue().set(key(nonce), String.valueOf(userId), Duration.ofSeconds(ttlSeconds));
        return nonce;
    }

    public Long consumeState(String nonce) {
        String k = key(nonce);
        String val = redis.opsForValue().get(k);
        if (val == null) return null;   // 만료/위조/재사용
        redis.delete(k);                // 1회성 소비
        try { return Long.valueOf(val); } catch (NumberFormatException e) { return null; }
    }
}
