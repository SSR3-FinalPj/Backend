package org.example.ssj3pj.services.google;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.config.GoogleOAuthClient;
import org.example.ssj3pj.dto.GoogleAccessTokenEvent;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.kafka.GoogleTokenKafkaProducer;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleTokenRepository googleTokenRepository;
    private final GoogleTokenKafkaProducer tokenKafkaProducer;
    private final GoogleOAuthClient googleOAuthClient; // 최초 1회 채널ID 조회용

    @Value("${google.client-id}")     private String clientId;
    @Value("${google.client-secret}") private String clientSecret;
    @Value("${google.redirect-uri}")  private String redirectUri;
    @Value("${google.token-uri}")     private String tokenUri;

    public void handleOAuthCallback(String code, Users user) {
        // 1) code -> 토큰 교환 (그대로)
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUri, HttpMethod.POST, request, Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Google OAuth 토큰 발급 실패: " + response.getStatusCode());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        String accessToken  = (String) body.get("access_token");
        String refreshToken = (String) body.get("refresh_token"); // 재동의 아니면 null 가능
        long   expiresIn    = ((Number) body.getOrDefault("expires_in", 3600)).longValue();
        Instant expiresAt   = Instant.now().plusSeconds(expiresIn);

        // 2) 토큰 업서트 (그대로)
        GoogleToken token = googleTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> GoogleToken.builder().user(user).build());
        token.setAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            token.setRefreshToken(refreshToken);
        }
        token.setExpiresAt(expiresAt);
        token = googleTokenRepository.save(token);

        // 3) ❗발행 전에 채널ID가 없으면 한 번 조회해서 저장
        if (token.getYoutubeChannelId() == null || token.getYoutubeChannelId().isBlank()) {
            try {
                var yt = googleOAuthClient.fetchMyChannel(accessToken); // Map<String,Object>
                String chId = yt != null ? (String) yt.get("channelId") : null;
                if (chId != null && !chId.isBlank()) {
                    token.setYoutubeChannelId(chId);
                    token = googleTokenRepository.save(token); // 저장 후 최신 상태로 사용
                }
            } catch (Exception ignore) {
                // 조회 실패 시 그대로 진행(이벤트엔 null이 들어갈 수 있음)
            }
        }

        // 4) ✅ 저장된 값으로 'issued' 이벤트 발행 (채널ID 포함)
        tokenKafkaProducer.publish(new GoogleAccessTokenEvent(
                user.getId(),
                "google",
                token.getAccessToken(),
                token.getExpiresAt().getEpochSecond(),
                "issued",
                token.getYoutubeChannelId() // ← DB 기준 최종값
        ));
    }

}
