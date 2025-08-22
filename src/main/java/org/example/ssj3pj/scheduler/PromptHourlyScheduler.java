package org.example.ssj3pj.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.Image;
import org.example.ssj3pj.repository.ImageRepository;
import org.example.ssj3pj.services.EnvironmentQueryService;
import org.example.ssj3pj.services.VideoPromptSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptHourlyScheduler {

    private final VideoPromptSender sender;
    private final ImageRepository imageRepository;
    private final EnvironmentQueryService environmentQueryService;

    @Value("${PROMPT_DEFAULT_ES_DOC_ID:latest}")
    private String defaultEsDocId;

    @Value("${PROMPT_TARGET_USER_ID:1}")
    private Long targetUserId;

    @Value("${ENABLE_SCHEDULER:true}")
    private boolean enableScheduler;

    /** 매일 08:00~18:00, 정각마다 실행 (Asia/Seoul) */
    @Scheduled(cron = "0 0 8-18 * * *", zone = "Asia/Seoul")
    public void runHourly() {
        if (!enableScheduler) return;
        try {
            log.info("[SCHED] hourly start -> userId={}, esDocId={}", targetUserId, defaultEsDocId);

            Optional<Image> latestImage = imageRepository.findTopByUser_IdOrderByCreatedAtDesc(targetUserId);
            String imagePath = latestImage.map(Image::getImagePath).orElse(null);

            if (imagePath == null) {
                log.warn("[SCHED] No recent image found for user ID: {}. Skipping scheduled video generation.", targetUserId);
                return;
            }

            // Get EnvironmentSummaryDto using defaultEsDocId
            EnvironmentSummaryDto summaryDto = environmentQueryService.getSummaryByDocId(defaultEsDocId);
            if (summaryDto == null) {
                log.error("[SCHED] No environment summary found for default ES Doc ID: {}. Skipping scheduled video generation.", defaultEsDocId);
                return;
            }

            // Set userId and imagePath in the DTO
            summaryDto.setUserId(targetUserId);
            summaryDto.setImagePath(imagePath);

            sender.sendEnvironmentDataToFastAPI(summaryDto, targetUserId, imagePath);
            log.info("[SCHED] hourly done");
        } catch (Exception e) {
            log.error("[SCHED] hourly failed", e);
        }
    }
}
