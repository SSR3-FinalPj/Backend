package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.User.RedditToken;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.RedditTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedditTokenGuard {

    private final RedditTokenRepository repo;
    private final RedditOAuthService oauth;

    /** API 호출 직전 유효한 액세스 토큰을 보장 */
    public String getValidAccessToken(Users user) {
        RedditToken t = repo.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Reddit not linked"));

        // 만료 60초 전이면 갱신
        if (t.getExpiresAt() != null && t.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return t.getAccessToken();
        }
        if (t.getRefreshToken() == null) {
            throw new IllegalStateException("No refresh token");
        }
        var r = oauth.refresh(t.getRefreshToken());
        t.setAccessToken(r.accessToken());
        t.setTokenType(r.tokenType());
        t.setExpiresAt(Instant.now().plusSeconds(r.expiresIn()));
        repo.save(t);
        return t.getAccessToken();
    }
}
