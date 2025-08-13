package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class VideoPromptSender {

    private final EnvironmentQueryService environmentQueryService; // ES -> DTO
    private final RestTemplate restTemplate;

    @Value("${BRIDGE_BASE_URL:http://127.0.0.1:8001}")  // 기본값 추가
    private String bridgeBaseUrl;

    /**
     * ES 문서 ID로 조회한 환경 요약 정보를 브릿지(FastAPI)로 전송
     */
    public void sendEnvironmentDataToFastAPI(String esDocId) {
        // 1) ES에서 DTO 조회
        EnvironmentSummaryDto dto = environmentQueryService.getSummaryByDocId(esDocId);

        // 2) 브릿지 엔드포인트
        String url = bridgeBaseUrl + "/api/generate-prompts";

        // 3) 전송
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, dto, String.class);
            System.out.println("✅ Bridge 응답: " + response.getBody());
        } catch (Exception e) {
            System.out.println("❌ Bridge 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
