package org.example.ssj3pj.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.response.GoogleTokenRefreshResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

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


    //accesstoken으로 youtubeid 스니펫형태로 가져오기
    public Map<String, Object> fetchMyChannel(String googleAccessToken) {
        // 1) 요청 URL 구성
        String url = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/youtube/v3/channels")
                .queryParam("part", "id,snippet")
                .queryParam("mine", "true")
                .build(true)
                .toUriString();

        // 2) Authorization 헤더에 구글 access_token 넣기
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleAccessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // 3) 호출
        ResponseEntity<JsonNode> res = restTemplate.exchange(
                url, HttpMethod.GET, req, JsonNode.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new RuntimeException("YouTube channels API 실패: " + res.getStatusCode());
        }

        JsonNode body = res.getBody();
        JsonNode items = body.path("items");
        if (!items.isArray() || items.size() == 0) {
            // 채널이 없는 계정일 수 있음
            return Map.of();
        }

        JsonNode ch = items.get(0);
        String channelId = ch.path("id").asText(null);
        String title     = ch.path("snippet").path("title").asText(null);
        String customUrl = ch.path("snippet").path("customUrl").asText(null);
        String country   = ch.path("snippet").path("country").asText(null);

        return new HashMap<>() {{
            put("channelId", channelId);
            put("title", title);
            put("customUrl", customUrl);
            put("country", country);
        }};
    }
}
