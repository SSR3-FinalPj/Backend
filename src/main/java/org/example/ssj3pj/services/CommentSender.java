package org.example.ssj3pj.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.request.CommentAnalysisRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentSender {

    @Qualifier("bridgeRestTemplate")
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @Value("${prompt.server.base}")
    private String bridgeBaseUrl;

    public JsonNode sendCommentsToAi(JsonNode Comments) {
        try {
            // youtubeComments는 이미 { "videoId": "...", "comments": [...] } 형태
            Map<String, Object> youtubeMap = objectMapper.convertValue(Comments, new TypeReference<>() {});

            CommentAnalysisRequest requestDto = CommentAnalysisRequest.builder()
                    .youtube(youtubeMap)
                    .build();

            String url = bridgeBaseUrl + "/api/comments";

            log.info("Sending request to AI: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestDto));
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestDto, String.class);
            log.info("Response from AI server: {}", response.getBody());

            // 응답 JSON 파싱
            JsonNode aiResponse = objectMapper.readTree(response.getBody());

            // top3 안의 platform 제거
            if (aiResponse.has("top3")) {
                for (JsonNode item : aiResponse.get("top3")) {
                    ((ObjectNode) item).remove("platform");
                }
            }

            return aiResponse;
        } catch (Exception e) {
            log.error("Failed to send comments to AI server", e);
            return objectMapper.createObjectNode(); // 에러시 빈 JSON
        }
    }
}
