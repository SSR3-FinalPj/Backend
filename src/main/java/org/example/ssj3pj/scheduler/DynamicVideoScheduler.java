package org.example.ssj3pj.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.dto.request.UserRequestData;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.example.ssj3pj.services.VideoPromptSender;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import java.util.Date;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class DynamicVideoScheduler {

    private final TaskScheduler taskScheduler;
    private final EnvironmentQueryService environmentQueryService;
    private final VideoPromptSender sender;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Map<Long, ScheduledFuture<?>> userTasks = new ConcurrentHashMap<>();

    /** 요청 들어왔을 때 스케줄링 시작 */
    public void startUserSchedule(Long userId, String esDocId) {
        if (userTasks.containsKey(userId)) {
            return; // 이미 돌고 있으면 무시
        }

        Runnable task = () -> {
            UserRequestData data = (UserRequestData) redisTemplate.opsForValue().get("video:request:" + userId);
            if (data == null) return;

            EnvironmentSummaryDto summaryDto = environmentQueryService.getSummaryByDocId(esDocId);
            if (summaryDto == null) return;

            summaryDto.setUserId(data.getUserId());
            summaryDto.setImageKey(data.getImageKey());

            sender.sendEnvironmentDataToFastAPI(summaryDto, data.getUserId(), data.getImageKey());
        };

        // 요청 시점 기준으로 1시간마다 실행
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                task,
                Date.from(Instant.now()).toInstant(), // 지금부터 시작
                Duration.ofHours(1)       // 매 1시간마다
        );

        userTasks.put(userId, future);

        // 18시에 멈추도록 예약
        scheduleStopAt18(userId);
    }

    /** 특정 userId의 스케줄링 멈춤 */
    public void stopUserSchedule(Long userId) {
        ScheduledFuture<?> future = userTasks.remove(userId);
        if (future != null) {
            future.cancel(true);
        }
    }

    /** 오늘 18시에 stop 예약 */
    private void scheduleStopAt18(Long userId) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime stopTime = now.withHour(18).withMinute(0).withSecond(0);

        if (stopTime.isBefore(now)) {
            stopTime = stopTime.plusDays(1); // 이미 18시가 지났다면 내일로
        }

        taskScheduler.schedule(() -> stopUserSchedule(userId),
                Date.from(stopTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()));
    }
}
