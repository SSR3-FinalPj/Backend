package org.example.ssj3pj.services;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.dto.PromptRequest;
import org.example.ssj3pj.dto.youtube.Top5VideoListDto;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.Prompt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.example.ssj3pj.repository.*;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.example.ssj3pj.services.ES.YoutubeQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final JobResultRepository jobResultRepository;
    private final YoutubeQueryService youtubeQueryService;
    private final UsersRepository usersRepository;
    private final YoutubeMetadataRepository youtubeMetadataRepository;
    /**
     * ES 문서 ID로 조회한 환경 요약 정보를 브릿지(FastAPI)로 전송
     * - 로그인한 사용자의 users.id를 DTO에 포함하여 전송
     */
    @Transactional
    public void sendEnvironmentDataToFastAPI(EnvironmentSummaryDto weatherData,
                                             Long jobId,
                                             Long userId,
                                             String imageKey,
                                             String promptText,
                                             String platform,
                                             boolean isInitial,
                                             boolean isClient) throws IOException {
        log.info("API START");
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        String beforePrompt;
        Prompt previousPrompt;
        PromptRequest element;
        PromptRequest sample = null;
        if (job.getParentResult() != null){
            beforePrompt = job.getParentResult().getJob().getPromptText();
            previousPrompt = promptRepository.findById(job.getParentResult().getJob().getId())
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            element = PromptRequest.builder()
                    .style(previousPrompt.getStyle())
                    .subject(previousPrompt.getSubject())
                    .focusAndLens(previousPrompt.getFocusAndLens())
                    .ambiance(previousPrompt.getAmbiance())
                    .cameraPositioning(previousPrompt.getCameraPositioning())
                    .composition(previousPrompt.getComposition())
                    .action(previousPrompt.getAction())
                    .build();
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("사용자 없음"));
            YoutubeMetadata metadata = youtubeMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + user));
            String esDocId = metadata.getEsDocId();
            Top5VideoListDto top5VideoListDto = youtubeQueryService.findTop5VideoByViews(esDocId);
            for (String video : top5VideoListDto.getVideos()){
                if(jobResultRepository.findByResultKey(video) == null) continue;
                else {
                    Prompt topPrompt = promptRepository.findById(jobResultRepository.findByResultKey(video).getJob().getId())
                            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
                    sample = PromptRequest.builder()
                            .style(topPrompt.getStyle())
                            .subject(topPrompt.getSubject())
                            .focusAndLens(topPrompt.getFocusAndLens())
                            .ambiance(topPrompt.getAmbiance())
                            .cameraPositioning(topPrompt.getCameraPositioning())
                            .composition(topPrompt.getComposition())
                            .action(topPrompt.getAction())
                            .build();
                    break;
                }
            }
        }
        else{
            beforePrompt = null;

            Random random = new Random();
            int rand = random.nextInt(4) + 1; // 1 ~ 4

            String style = null;
            String subject = null;
            String focusAndLens = null;
            String ambiance = null;
            String cameraPositioning = null;
            String composition = null;
            String action = null;

            switch (rand) {
                case 1 -> {
                    style = "Cinematic";
                    subject = "Urban street scene";
                    focusAndLens = "Wide-angle lens";
                    ambiance = "Moody night atmosphere";
                    cameraPositioning = "Low-angle shot";
                    composition = "Rule of thirds";
                    action = "Walking crowd";
                }
                case 2 -> {
                    style = "Minimalist";
                    subject = "Nature landscape";
                    focusAndLens = "Telephoto lens";
                    ambiance = "Bright and calm";
                    cameraPositioning = "Bird-eye view";
                    composition = "Symmetry focus";
                    action = "Flowing river";
                }
                case 3 -> {
                    style = "Realistic";
                    subject = "Portrait";
                    focusAndLens = "50mm lens";
                    ambiance = "Soft warm light";
                    cameraPositioning = "Eye-level";
                    composition = "Centered";
                    action = "Smiling";
                }
                case 4 -> {
                    style = "Fantasy art";
                    subject = "Mystical forest";
                    focusAndLens = "Fish-eye lens";
                    ambiance = "Dreamy mist";
                    cameraPositioning = "Tilted shot";
                    composition = "Dynamic diagonal";
                    action = "Flying creatures";
                }
            }

            element = PromptRequest.builder()
                    .style(style)
                    .subject(subject)
                    .focusAndLens(focusAndLens)
                    .ambiance(ambiance)
                    .cameraPositioning(cameraPositioning)
                    .composition(composition)
                    .action(action)
                    .build();
        }

        VideoGenerationRequestDto requestDto = VideoGenerationRequestDto.builder()
                .jobId(jobId)
                .imageKey(imageKey)
                .mascotImageKey(job.getMascotImageKey())
                .currentPrompt(promptText)
                .isClient(isClient)
                .platform(platform)
                .weatherData(weatherData)
                .beforePrompt(beforePrompt)
                .element(element)
                .sample(sample)
                .build();

        log.info("Build END");
//        String url = bridgeBaseUrl + "/api/generate-media";
        String url = bridgeBaseUrl + "/api/veo3-generate";
        try {
            log.info("Bridge START");
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestDto, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode extracted = root.path("extracted");
            if(isClient){
                log.info("✅ Client 확인 Prompt 저장 시작: isClient={}", isClient);
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
            }

            log.info("✅ Bridge 응답: {}", response.getBody()  );
        } catch (Exception e) {
            log.error("❌ Bridge 전송 실패: {}", e.getMessage(), e);
        }
    }
}
