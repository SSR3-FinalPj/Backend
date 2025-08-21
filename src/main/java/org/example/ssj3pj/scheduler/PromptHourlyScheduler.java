// sched/PromptHourlyScheduler.java
package org.example.ssj3pj.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.services.VideoPromptSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptHourlyScheduler {

    private final VideoPromptSender sender;

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
            sender.sendEnvironmentDataToFastAPI(defaultEsDocId, targetUserId);
            log.info("[SCHED] hourly done");
        } catch (Exception e) {
            log.error("[SCHED] hourly failed", e);
        }
    }
}
