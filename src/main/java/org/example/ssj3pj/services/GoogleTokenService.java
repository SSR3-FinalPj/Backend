package org.example.ssj3pj.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.example.ssj3pj.config.GoogleOAuthClient;
import org.example.ssj3pj.dto.response.GoogleTokenRefreshResponse;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private final GoogleTokenRepository googleTokenRepository;
    private final GoogleOAuthClient googleOAuthClient;

    @Value("${google.skew-seconds:120}")
    private long skewSeconds;

    /**
     * 유튜브 API 호출 직전에 호출:
     * - 만료 임박/만료면 refresh로 재발급
     * - 유효하면 기존 access_token 그대로
     */
    @Transactional
    public String getValidAccessToken(Long userId) {
        // 동시 갱신 방지를 위한 행잠금
        GoogleToken token = googleTokenRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("유저의 Google 연동 정보가 없습니다. userId=" + userId));

//        if (!"LINKED".equals(token.getStatus())) {
//            throw new IllegalStateException("Google 연동이 유효하지 않습니다. status=" + token.getStatus());
//        }

        Instant now = Instant.now();
        Instant threshold = now.plusSeconds(skewSeconds);

        if (token.getExpiresAt().isAfter(threshold)) {
            // 아직 유효
            return token.getAccessToken();
        }

        // 만료(또는 임박) → refresh
        GoogleTokenRefreshResponse res = refreshWith(token.getRefreshToken());

        token.setAccessToken(res.access_token());
        token.setExpiresAt(now.plusSeconds(res.expires_in()));
        token.setUpdatedAt(Instant.now());

        // 드물게 refresh_token 회전 시 응답에 새 refresh_token이 담김
        if (res.refresh_token() != null && !res.refresh_token().isBlank()) {
            token.setRefreshToken(res.refresh_token());
        }

        // JPA @Transactional: flush on commit
        return token.getAccessToken();
    }

    private GoogleTokenRefreshResponse refreshWith(String refreshToken) {
        try {
            return googleOAuthClient.refreshAccessToken(refreshToken);
        } catch (Exception e) {
            // invalid_grant 등으로 실패 → 상태 전환
            // (실제 구현에선 예외 타입/메시지 파싱해서 조건부 처리 권장)
            throw new IllegalStateException("Google 토큰 재발급 실패: 재연동 필요", e);
        }
    }
}
