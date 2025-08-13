package org.example.ssj3pj.services;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    // 저장
    public void saveRefreshToken(String username, String refreshToken, long expirationSeconds) {
        redisTemplate.opsForValue().set(username, refreshToken, Duration.ofSeconds(expirationSeconds));
    }

    // 검증
    public boolean isValidRefreshToken(String username, String token) {
        String stored = redisTemplate.opsForValue().get(username);
        return token.equals(stored);
    }

    // 삭제 (로그아웃 시)
    public void deleteRefreshToken(String username) {
        redisTemplate.delete(username);
    }
}
