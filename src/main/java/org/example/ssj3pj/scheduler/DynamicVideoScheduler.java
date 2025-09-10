package org.example.ssj3pj.scheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.dto.request.UserRequestData;
import org.example.ssj3pj.redis.VideoRequestService;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.example.ssj3pj.services.VideoPromptSender;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicVideoScheduler {

    private final TaskScheduler taskScheduler;
    private final EnvironmentQueryService environmentQueryService;
    private final VideoPromptSender sender;
    private final VideoRequestService videoRequestService;

    /**
     * 최초 요청 → 즉시 4개 생성
     */
    public void startInitialJob(Long jobId) {
        for (int i = 0; i < 4; i++) {
            runTask(jobId, true);
        }
        log.info("[SCHED] 최초 요청 Job {} → 4개 영상 즉시 생성 완료", jobId);
    }

    /**
     * 수정 요청 → 10분 간격으로 4개 생성
     */
    public void startRevisionJob(Long jobId) {
        for (int i = 0; i < 4; i++) {
            int delayMinutes = i * 10;
            taskScheduler.schedule(
                    () -> runTask(jobId, false),
                    Date.from(Instant.now().plus(Duration.ofMinutes(delayMinutes)))
            );
        }
        log.info("[SCHED] 수정 요청 Job {} → 10분 간격으로 4개 영상 생성 예약 완료", jobId);
    }

    /**
     * 실제 실행 로직
     */
    private void runTask(Long jobId, boolean isClient) {
        UserRequestData data = videoRequestService.getJobRequest(jobId);
        if (data == null) {
            log.warn("[SCHED] No request data in Redis for job {}", jobId);
            return;
        }

        EnvironmentSummaryDto summary = environmentQueryService.getRecentSummaryByLocation(data.getLocationCode());
        if (summary == null) {
            log.warn("[SCHED] No ES data for locationCode={} job={}", data.getLocationCode(), jobId);
            return;
        }

        try {
            sender.sendEnvironmentDataToFastAPI(
                    summary,
                    jobId,
                    data.getUserId(),
                    data.getImageKey(),
                    data.getPrompttext(),
                    data.getPlatform(),
                    isClient
            );
            log.info("[SCHED] Sent video request for job={}, user={}, isClient={}", jobId, data.getUserId(), isClient);
        } catch (Exception e) {
            log.error("[SCHED] Failed to send video request for job {}", jobId, e);
        }
    }
}
