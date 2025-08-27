package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.reddit.RedditMeDto;
import org.example.ssj3pj.dto.reddit.TokenResponseDto;
import org.example.ssj3pj.entity.User.RedditToken;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.RedditTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditOAuthService {

    // 필요 시 @Qualifier("redditRestTemplate") 사용
    private final RestTemplate restTemplate;
    private final RedditTokenRepository tokenRepo;

    @Value("${reddit.client-id}")     String clientId;
    @Value("${reddit.client-secret}") String clientSecret;
    @Value("${reddit.redirect-uri}")  String redirectUri;
    @Value("${reddit.user-agent}")    String userAgent;
    @Value("${reddit.token-url}")     String tokenUrl;   // https://www.reddit.com/api/v1/access_token
    @Value("${reddit.api-base}")      String apiBase;    // https://oauth.reddit.com

    /** 콜백에서 code를 받아 토큰 교환 → reddit_token 업서트 */
    public void handleOAuthCallback(String code, Users user) {
        TokenResponseDto token = exchangeCode(code);
        RedditMeDto me = fetchIdentity(token.getAccessToken());

        RedditToken row = tokenRepo.findByUserId(user.getId())
                .orElseGet(() -> RedditToken.builder().user(user).build());

        row.setRedditUserId(me.getId());
        row.setRedditUsername(me.getName());
        row.setAccessToken(token.getAccessToken());
        row.setRefreshToken(token.getRefreshToken());
        row.setTokenType(token.getTokenType());
        row.setExpiresAt(Instant.now().plusSeconds(token.getExpiresIn()));

        tokenRepo.save(row);
    }

    /** 간단 연동 상태 */
    public Map<String, Object> simpleStatus(Long userId) {
        return tokenRepo.findByUserId(userId)
                .<Map<String,Object>>map(t -> Map.of(
                        "linked", true,
                        "username", t.getRedditUsername(),
                        "expiresAt", t.getExpiresAt()
                ))
                .orElse(Map.of("linked", false));
    }

    /** 액세스 토큰 만료 시 리프레시 */
    public TokenResponseDto refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret, StandardCharsets.UTF_8);
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> res = restTemplate.exchange(tokenUrl, HttpMethod.POST, req, Map.class);
            return TokenResponseDto.from(res.getBody());
        } catch (RestClientResponseException e) {
            log.error("Reddit refresh failed: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /* -------------------- 내부 헬퍼 -------------------- */

    private TokenResponseDto exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret, StandardCharsets.UTF_8);
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> res = restTemplate.exchange(tokenUrl, HttpMethod.POST, req, Map.class);
            return TokenResponseDto.from(res.getBody());
        } catch (RestClientResponseException e) {
            log.error("Reddit token exchange failed: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private RedditMeDto fetchIdentity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set(HttpHeaders.USER_AGENT, userAgent);

        HttpEntity<Void> req = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> res = restTemplate.exchange(apiBase + "/api/v1/me", HttpMethod.GET, req, Map.class);
            return RedditMeDto.from(res.getBody());
        } catch (RestClientResponseException e) {
            log.error("Reddit /me failed: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /* (옵션) 토큰 폐기 */
    public void revokeRefreshToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret, StandardCharsets.UTF_8);
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        try {
            restTemplate.exchange("https://www.reddit.com/api/v1/revoke_token",
                    HttpMethod.POST, new HttpEntity<>(form, headers), Void.class);
        } catch (RestClientResponseException e) {
            // 200/400 혼재 가능 — 운영 정책에 맞게 무시 또는 로깅
            log.warn("Reddit revoke response: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
        }
    }

    /* (참고) Basic 값 만들 때 직접 Base64가 필요하면 */
    @SuppressWarnings("unused")
    private String basicAuthLegacy() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );
    }
}
