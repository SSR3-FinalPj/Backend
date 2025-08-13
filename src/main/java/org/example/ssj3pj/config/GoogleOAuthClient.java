package org.example.ssj3pj.config;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.response.GoogleTokenRefreshResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleTokenRefreshResponse refreshAccessToken(String refreshToken) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var body = new LinkedMultiValueMap<String, String>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        var req = new HttpEntity<>(body, headers);

        ResponseEntity<GoogleTokenRefreshResponse> res =
            restTemplate.exchange(tokenUri, HttpMethod.POST, req, GoogleTokenRefreshResponse.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new RuntimeException("Google refresh API 실패: " + res.getStatusCode());
        }
        return res.getBody();
    }
}
