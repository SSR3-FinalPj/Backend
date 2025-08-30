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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicVideoScheduler {

    private final TaskScheduler taskScheduler;
    private final EnvironmentQueryService environmentQueryService;
    private final VideoPromptSender sender;
    private final VideoRequestService videoRequestService;

    private final Map<Long, ScheduledFuture<?>> jobTasks = new ConcurrentHashMap<>();

    /**
     * 요청 들어왔을 때 스케줄링 시작 (기존 스케줄 있으면 중단하고 새로 시작)
     */
    public void startJobSchedule(Long jobId) {
        stopJobSchedule(jobId);

        // ✅ 즉시 실행 (클라이언트 요청)
        runTask(jobId, true);

        // ✅ 1시간마다 반복 실행 (스케줄러 실행)
        Runnable scheduledTask = () -> runTask(jobId, false);

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                scheduledTask,
                Date.from(Instant.now().plusSeconds(3600)),
                Duration.ofHours(1).toMillis()
        );

        jobTasks.put(jobId, future);

        scheduleStopAt18(jobId);
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
                    data.getUserId(),
                    data.getImageKey(),
                    data.getPrompttext(),
                    isClient // ✅ 클라이언트 실행 여부 전달
            );
            log.info("[SCHED] Sent video request for job={}, user={}, isClient={}", jobId, data.getUserId(), isClient);
        } catch (Exception e) {
            log.error("[SCHED] Failed to send video request for job {}", jobId, e);
        }
    }

    /**
     * 특정 jobId의 스케줄링 중단
     */
    public void stopJobSchedule(Long jobId) {
        ScheduledFuture<?> future = jobTasks.remove(jobId);
        if (future != null) {
            future.cancel(true);
            log.info("[SCHED] Stopped schedule for jobId={}", jobId);
        }
    }

    /**
     * 오늘 18시에 stop 예약
     */
    private void scheduleStopAt18(Long jobId) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime stopTime = now.withHour(18).withMinute(0).withSecond(0);

        if (stopTime.isBefore(now)) {
            stopTime = stopTime.plusDays(1);
        }

        taskScheduler.schedule(() -> stopJobSchedule(jobId),
                Date.from(stopTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()));
    }
}
