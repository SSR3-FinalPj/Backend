package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// 8.10 월요일에 수정해야함 - FastAPI 엔드포인트 맞추기
@Service
@RequiredArgsConstructor
public class VideoPromptSender {

    private final EnvironmentQueryService environmentQueryService;
    private final RestTemplate restTemplate;

    @Value("${AI_VIDEO_SERVER_URL}")
    private String aiVideoServerUrl;

    public void sendEnvironmentDataToFastAPI(String esDocId) {
        // 1. ES에서 환경 데이터 조회
        EnvironmentSummaryDto dto = environmentQueryService.getEnvironmentByDocId(esDocId);

        // 2. FastAPI 서버 URL + 엔드포인트
        String url = aiVideoServerUrl + "/api/generate-prompt";

        // 3. 전송
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, dto, String.class);
            System.out.println("✅ FastAPI 응답: " + response.getBody());
        } catch (Exception e) {
            System.out.println("❌ FastAPI 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
