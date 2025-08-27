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

import java.time.*;
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

    // 🔄 jobId별 스케줄 관리
    private final Map<Long, ScheduledFuture<?>> jobTasks = new ConcurrentHashMap<>();

    /**
     * 요청 들어왔을 때 스케줄링 시작 (기존 스케줄 있으면 중단하고 새로 시작)
     */
    public void startJobSchedule(Long jobId, Long userId) {
        // 기존 스케줄 있으면 중단
        stopJobSchedule(jobId);

        Runnable task = () -> {
            UserRequestData data = videoRequestService.getUserRequest(jobId);
            if (data == null) {
                log.warn("[SCHED] No request data in Redis for jobId={}", jobId);
                return;
            }

            // 매 실행 시점마다 최신 환경 데이터 조회
            EnvironmentSummaryDto summaryDto = environmentQueryService.getSummaryByDocId(data.getLocationCode());
            if (summaryDto == null) {
                log.warn("[SCHED] No ES data found for locationCode={} jobId={}", data.getLocationCode(), jobId);
                return;
            }

            // 요청 시점에 고정된 jobId + imageKey 세팅
            summaryDto.setUserId(data.getUserId());
            summaryDto.setImageKey(data.getImageKey());

            try {
                sender.sendEnvironmentDataToFastAPI(summaryDto, jobId, data.getImageKey());
                log.info("[SCHED] Sent video request for jobId={}, imageKey={}", jobId, data.getImageKey());
            } catch (Exception e) {
                log.error("[SCHED] Failed to send video request for jobId={}", jobId, e);
            }
        };

        // 요청 즉시 실행 → 이후 1시간마다 반복
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                task,
                Date.from(Instant.now()),
                Duration.ofHours(1).toMillis()
        );

        jobTasks.put(jobId, future);

        // 18시에 자동 종료 예약
        scheduleStopAt18(jobId);
        log.info("[SCHED] Started hourly schedule for jobId={} (userId={})", jobId, userId);
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
            stopTime = stopTime.plusDays(1); // 이미 18시가 지났으면 내일로
        }

        taskScheduler.schedule(() -> stopJobSchedule(jobId),
                Date.from(stopTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()));
    }
}
