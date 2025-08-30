package org.example.ssj3pj.services.Reddit;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.GoogleAccessTokenEvent;
import org.example.ssj3pj.entity.User.RedditToken;
import org.example.ssj3pj.kafka.GoogleTokenKafkaProducer;
import org.example.ssj3pj.repository.RedditTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedditTokenGuard {

    private final RedditTokenRepository repo;
    private final RedditOAuthService oauth;
    private final GoogleTokenKafkaProducer tokenProducer;

    @Value("${reddit.skew-seconds:60}")
    private long skewSeconds;

    /** API 호출 직전 유효한 액세스 토큰을 보장 */
    @Transactional
    public String getValidAccessToken(Long userId) {
        // 동시 갱신 방지를 위해 for update 쿼리 사용
        RedditToken t = repo.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Reddit not linked"));

        Instant now = Instant.now();
        Instant threshold = now.plusSeconds(skewSeconds);

        // 아직 충분히 유효하면 그대로 반환
        if (t.getExpiresAt() != null && t.getExpiresAt().isAfter(threshold)) {
            return t.getAccessToken();
        }

        if (t.getRefreshToken() == null || t.getRefreshToken().isBlank()) {
            throw new IllegalStateException("No refresh token: 재연동 필요");
        }

        // refresh
        var r = oauth.refresh(t.getRefreshToken());

        t.setAccessToken(r.getAccessToken());
        t.setTokenType(r.getTokenType());
        long expiresIn = r.getExpiresIn() != null ? r.getExpiresIn() : 3600L;
        t.setExpiresAt(now.plusSeconds(expiresIn));
        t.setUpdatedAt(Instant.now());

        // Kafka 이벤트 발행 (스케줄링 서버가 소비)
        tokenProducer.publish(new GoogleAccessTokenEvent(
                t.getUser().getId(),
                "reddit",                              // provider 구분
                t.getAccessToken(),
                t.getExpiresAt().getEpochSecond(),
                "refreshed",
                t.getRedditUsername()                  // extra: Reddit username
        ));

        return t.getAccessToken();
    }
}
