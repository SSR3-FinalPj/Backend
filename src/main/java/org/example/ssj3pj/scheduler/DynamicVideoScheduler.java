package org.example.ssj3pj.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.dto.request.UserRequestData;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.redis.VideoRequestService;
import org.example.ssj3pj.repository.JobRepository;
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
    private final JobRepository jobRepository;
    private final EnvironmentQueryService environmentQueryService;
    private final VideoPromptSender sender;
    private final VideoRequestService videoRequestService;

    /**
     * 최초 요청 → 첫 번째 영상 즉시 생성
     */
    public void startInitialJob(Long jobId) {
        triggerNext(jobId, true, 0); // step=0에서 시작
        log.info("[SCHED] 최초 요청 Job {} → 첫 번째 영상 생성 시작", jobId);
    }

    /**
     * 수정 요청 → 첫 번째 영상 즉시 생성
     */
    public void startRevisionJob(Long jobId) {
        triggerNext(jobId, false, 0);
        log.info("[SCHED] 수정 요청 Job {} → 첫 번째 영상 생성 시작", jobId);
    }

    /**
     * 다음 영상 생성 트리거
     * 최초 요청은 즉시 실행, 수정 요청은 10분 지연 가능
     */
    public void triggerNext(Long jobId, boolean isInitial, int currentStep) {
        int delayMinutes = 0;
        if (!isInitial) {
            delayMinutes = 0; // 수정 요청은 10분 간격
        }

        taskScheduler.schedule(
                () -> runTask(jobId, isInitial, currentStep + 1),
                Date.from(Instant.now().plus(Duration.ofMinutes(delayMinutes)))
        );
    }

    /**
     * 실제 실행 로직
     */
    private void runTask(Long jobId, boolean isInitial, int step) {
        UserRequestData data = videoRequestService.getJobRequest(jobId);
        if (data == null) {
            log.warn("[SCHED] No request data in Redis for job {}", jobId);
            return;
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("job not found by Id : " + jobId));
        EnvironmentSummaryDto summary = null;

        if ("COMPLETED".equals(job.getStatus())) {
            log.info("[SCHED] Job {} already completed, skipping step {}", jobId, step);
            return;
        }

        // ✅ null-safe 체크
        if (Boolean.TRUE.equals(job.getUseCitydata())) {
            log.info("Citydata used!");
            summary = environmentQueryService.getRecentSummaryByLocation(data.getLocationCode());
            if (summary == null) {
                log.warn("[SCHED] No ES data for locationCode={} job={}", data.getLocationCode(), jobId);
                return;
            }
        } else {
            log.info("Citydata not used or null!");
        }

        boolean isClient = (step == 1);

        try {
            sender.sendEnvironmentDataToFastAPI(
                    summary,
                    jobId,
                    data.getUserId(),
                    data.getImageKey(),
                    data.getPrompttext(),
                    data.getPlatform(),
                    isInitial,
                    isClient
            );
            log.info("[SCHED] Sent video request for job={}, step={}, isInitial={}", jobId, step, isInitial);
        } catch (Exception e) {
            log.error("[SCHED] Failed to send video request for job {}", jobId, e);
        }
    }

}
