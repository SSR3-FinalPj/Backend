package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.config.GoogleOAuthClient;
import org.example.ssj3pj.dto.GoogleAccessTokenEvent;
import org.example.ssj3pj.dto.response.GoogleTokenRefreshResponse;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.example.ssj3pj.kafka.GoogleTokenKafkaProducer;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private final GoogleTokenRepository tokenRepo;
    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleTokenKafkaProducer tokenProducer;

    @Value("${google.skew-seconds}")
    private long skewSeconds;

    @Transactional //dirtychecking
    public String getValidAccessToken(Long userId) {
        // 동시 갱신 방지: for update
        GoogleToken token = tokenRepo.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Google 연동 정보 없음. userId=" + userId));

        Instant now = Instant.now();
        Instant threshold = now.plusSeconds(skewSeconds);

        // 만료 시간이 없거나 임박/만료이면 refresh, 아니면 그대로 반환
        if (token.getExpiresAt() != null && token.getExpiresAt().isAfter(threshold)) {
            return token.getAccessToken();
        }

        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            throw new IllegalStateException("refresh_token 없음: 재연동 필요. userId=" + userId);
        }

        GoogleTokenRefreshResponse res = refreshWith(token.getRefreshToken());

        token.setAccessToken(res.access_token());
        long expiresIn = res.expires_in() != null ? res.expires_in() : 3600L;
        token.setExpiresAt(now.plusSeconds(expiresIn));
        token.setUpdatedAt(Instant.now());

        // 카프카 이벤트: DB에 저장된 youtubeChannelId 사용
        tokenProducer.publish(new GoogleAccessTokenEvent(
                token.getUser().getId(),
                "google",
                token.getAccessToken(),
                token.getExpiresAt().getEpochSecond(),
                "refreshed",
                token.getYoutubeChannelId()
        ));

        // @Transactional 이므로 커밋 시 flush
        return token.getAccessToken();
    }

    private GoogleTokenRefreshResponse refreshWith(String refreshToken) {
        try {
            return googleOAuthClient.refreshAccessToken(refreshToken);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Google 토큰 갱신 실패: 재연동 필요", e);
        }
    }
}
