package org.example.ssj3pj.services;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.example.ssj3pj.dto.VideoGenerationRequestDto;

// 프롬프트 전송
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoPromptSender {

    @Qualifier("bridgeRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${BRIDGE_BASE_URL}")
    private String bridgeBaseUrl;

    /**
     * ES 문서 ID로 조회한 환경 요약 정보를 브릿지(FastAPI)로 전송
     * - 로그인한 사용자의 users.id를 DTO에 포함하여 전송
     */
    public void sendEnvironmentDataToFastAPI(EnvironmentSummaryDto weatherData,
                                             Long userId,
                                             String imageKey,
                                             String promptText,
                                             boolean isClient) {
        log.info("API START");
        VideoGenerationRequestDto requestDto = VideoGenerationRequestDto.builder()
                .imageKey(imageKey)
                .userId(String.valueOf(userId)) // Convert Long to String
                .promptText(promptText)
                .isClient(isClient)
                .platform("youtube")
                .weatherData(weatherData)
                .userData(Map.of())
                .build();

        log.info("Build END");
        String url = bridgeBaseUrl + "/api/generate-video";
        try {
            log.info("Bridge START");
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestDto, String.class);
            log.info("✅ Bridge 응답: {}", response.getBody());
        } catch (Exception e) {
            log.error("❌ Bridge 전송 실패: {}", e.getMessage(), e);
        }
    }
}
