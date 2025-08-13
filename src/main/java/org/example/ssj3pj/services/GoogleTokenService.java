package org.example.ssj3pj.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.config.GoogleOAuthClient;
import org.example.ssj3pj.dto.GoogleAccessTokenEvent;
import org.example.ssj3pj.dto.response.GoogleTokenRefreshResponse;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.example.ssj3pj.kafka.GoogleTokenKafkaProducer;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private final GoogleTokenRepository tokenRepo;
    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleTokenKafkaProducer tokenProducer;

    @Value("${google.skew-seconds}")
    private long skewSeconds;

    /**
     * 유튜브 API 호출 직전에 사용:
     * - 만료 임박(<= skew) 또는 만료면 refresh
     * - 유효하면 기존 access_token 반환
     */
    @Transactional
    public String getValidAccessToken(Long userId) {
        // 동시 갱신 방지: for update (레포에 쿼리 필요)
        GoogleToken token = tokenRepo.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Google 연동 정보 없음. userId=" + userId));

        Instant now = Instant.now();
        Instant threshold = now.plusSeconds(skewSeconds);

        // 아직 충분히 유효하면 그대로 반환
        if (token.getExpiresAt() != null && token.getExpiresAt().isAfter(threshold)) {
            return token.getAccessToken();
        }

        // 만료 또는 임박 → refresh
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            throw new IllegalStateException("refresh_token 없음: 재연동 필요. userId=" + userId);
        }

        GoogleTokenRefreshResponse res = refreshWith(token.getRefreshToken());

        token.setAccessToken(res.access_token());
        token.setExpiresAt(now.plusSeconds(
                res.expires_in() != null ? res.expires_in() : 3600L
        ));
        token.setUpdatedAt(Instant.now());

        // 드물게 새 refresh_token이 올 수 있어서 교체
        if (res.refresh_token() != null && !res.refresh_token().isBlank()) {
            token.setRefreshToken(res.refresh_token());
        }

        tokenProducer.publish(new GoogleAccessTokenEvent(
                token.getUser().getId(),
                "google",
                token.getAccessToken(),
                token.getExpiresAt().getEpochSecond(),
                "refreshed"
        ));
        // @Transactional 이므로 커밋 시점에 flush
        return token.getAccessToken();
    }

    private GoogleTokenRefreshResponse refreshWith(String refreshToken) {
        try {
            return googleOAuthClient.refreshAccessToken(refreshToken);
        } catch (RuntimeException e) {
            // 여기서 invalid_grant 구분하고 싶으면 GoogleOAuthClient에서 메시지 파싱해 별도 예외 던지도록 개선
            throw new IllegalStateException("Google 토큰 갱신 실패: 재연동 필요", e);
        }
    }
}
