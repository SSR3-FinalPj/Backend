package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleTokenRepository googleTokenRepository;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @Value("${google.token-uri}")
    private String tokenUri;

    public void handleOAuthCallback(String code, Users user) {
        // 1. access_token 요청
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            Map body = response.getBody();

            String accessToken = (String) body.get("access_token");
            String refreshToken = (String) body.get("refresh_token");
            Integer expiresIn = (Integer) body.get("expires_in");

            Instant expiresAt = Instant.now().plusSeconds(expiresIn);

            GoogleToken token = GoogleToken.builder()
                    .user(user)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .build();

            googleTokenRepository.save(token);
        } else {
            throw new RuntimeException("Google OAuth 토큰 발급 실패");
        }
    }
}
