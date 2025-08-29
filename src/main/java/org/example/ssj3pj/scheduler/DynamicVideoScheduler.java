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

    private final Map<Long, ScheduledFuture<?>> jobTasks = new ConcurrentHashMap<>();

    /**
     * 요청 들어왔을 때 스케줄링 시작 (기존 스케줄 있으면 중단하고 새로 시작)
     */
    public void startJobSchedule(Long jobId, Long userId) {
        // 기존 스케줄 있으면 중단
        stopJobSchedule(jobId);

        Runnable task = () -> {
            UserRequestData data = videoRequestService.getJobRequest(jobId);
            if (data == null) {
                log.warn("[SCHED] No request data in Redis for job {}", jobId);
                return;
            }

            // 매 실행마다 최신 환경 데이터 가져오기
            EnvironmentSummaryDto summary = environmentQueryService.getRecentSummaryByLocation(data.getLocationCode());
            if (summary == null) {
                log.warn("[SCHED] No ES data for locationCode={} job={}", data.getLocationCode(), jobId);
                return;
            }

            // FastAPI 전송
            sender.sendEnvironmentDataToFastAPI(summary, data.getUserId(), data.getImageKey());
        };

        // 즉시 실행
        task.run();

        // 1시간마다 반복 실행
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                task,
                Date.from(Instant.now().plusSeconds(3600)), // 1시간 뒤부터
                Duration.ofHours(1).toMillis()
        );

        jobTasks.put(jobId, future);

        // 18시에 자동 종료 예약
        scheduleStopAt18(jobId);
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
