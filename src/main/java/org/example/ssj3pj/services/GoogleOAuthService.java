package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleTokenRepository googleTokenRepository;
    private final GoogleTokenKafkaProducer tokenKafkaProducer;

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

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

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

            //accesstoken 토큰 발급 기준으로 카프카 발행
            GoogleAccessTokenEvent evt = new GoogleAccessTokenEvent(
                    user.getId(),
                    "google",
                    accessToken,
                    expiresAt.getEpochSecond(),
                    "issued"
            );
            tokenKafkaProducer.publish(evt);
        } else {
            throw new RuntimeException("Google OAuth 토큰 발급 실패");
        }
    }
}
