package org.example.ssj3pj.services;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.Prompt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.PromptRepository;
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
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final PromptRepository promptRepository;
    /**
     * ES 문서 ID로 조회한 환경 요약 정보를 브릿지(FastAPI)로 전송
     * - 로그인한 사용자의 users.id를 DTO에 포함하여 전송
     */
    public void sendEnvironmentDataToFastAPI(EnvironmentSummaryDto weatherData,
                                             Long jobId,
                                             Long userId,
                                             String imageKey,
                                             String promptText,
                                             String platform,
                                             boolean isClient) {
        log.info("API START");
        VideoGenerationRequestDto requestDto = VideoGenerationRequestDto.builder()
                .jobId(jobId)
                .imageKey(imageKey)
                .userId(String.valueOf(userId)) // Convert Long to String
                .promptText(promptText)
                .isClient(isClient)
                .platform(platform)
                .weatherData(weatherData)
                .userData(Map.of())
                .build();

        log.info("Build END");
        String url = bridgeBaseUrl + "/api/generate-video";
        try {
            log.info("Bridge START");
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestDto, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode extracted = root.path("extracted");

            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            Prompt prompt = Prompt.builder()
                    .job(job)
                    .subject(extracted.path("subject").asText(null))
                    .action(extracted.path("Action").asText(null))
                    .style(extracted.path("Style").asText(null))
                    .cameraPositioning(extracted.path("Camera positioning and motion").asText(null))
                    .composition(extracted.path("Composition").asText(null))
                    .focusAndLens(extracted.path("Focus and lens effects").asText(null))
                    .ambiance(extracted.path("Ambiance").asText(null))
                    .build();

            promptRepository.save(prompt);
            log.info("✅ Prompt 저장 완료: jobId={}", jobId);
            log.info("✅ Bridge 응답: {}", response.getBody()  );
        } catch (Exception e) {
            log.error("❌ Bridge 전송 실패: {}", e.getMessage(), e);
        }
    }
}
