package org.example.ssj3pj.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.config.SseConfig.SseSettings;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;   // ★ 추가
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;                                      // ★ 추가
import java.util.Set;
import java.util.concurrent.*;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Component
@RequiredArgsConstructor
public class SseHub {

    private static final String INIT_EVENT = "init";
    private static final String VIDEO_READY_EVENT = "video-ready";
    private static final String PING_EVENT = "ping";          // ★ 추가
    private static final long TIMEOUT_MILLIS = 30L * 60 * 1000;
    private static final int MAX_CONN_PER_USER = 5;

    private final SseSettings conf;

    // userId -> emitters
    private final ConcurrentMap<Long, CopyOnWriteArraySet<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        var set = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>());
        if (set.size() >= MAX_CONN_PER_USER) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Too many SSE connections");
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        set.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name(INIT_EVENT)
                    .data("ok")
                    .reconnectTime(conf.reconnectMillis()));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    /** Kafka에서 호출: 메시지 + 타임스탬프만 전송 */
    public void notifyVideoReady(Long userId, String type) {
        var payload = new SimpleMsg("VIDEO_READY", Instant.now().toString(),type);
        send(userId, VIDEO_READY_EVENT, payload);
    }

    /** YouTube 업로드 완료 알림 */
    public void notifyYoutubeUploadCompleted(Long userId, String videoId) {
        var payload = Map.of(
            "videoUrl", "https://www.youtube.com/watch?v=" + videoId,
            "timestamp", Instant.now().toString()
        );
        send(userId, "youtube-upload-completed", payload);
    }

    /** Reddit 업로드 완료 알림 */
    public void notifyRedditUploadCompleted(Long userId, String postId) {
        var payload = Map.of(
                "postUrl", "https://www.reddit.com/comments/" + postId,
                "timestamp", Instant.now().toString()
        );
        send(userId, "reddit-upload-completed", payload);
    }
    /* 일정 주기로 모든 연결에 ping 전송 */
    @Scheduled(fixedDelayString = "${sse.heartbeat-millis:25000}")
    public void heartbeat() {

        if (emitters.isEmpty()) return;
        for (Map.Entry<Long, CopyOnWriteArraySet<SseEmitter>> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            for (SseEmitter em : entry.getValue()) {
                try {
                    em.send(SseEmitter.event()
                            .name(PING_EVENT)
                            .data("pong") // 또는 ISO 시간: Instant.now().toString()
                            .reconnectTime(conf.reconnectMillis()));
                } catch (IOException e) {
                    remove(userId, em);
                }
            }
        }
    }

    private void send(Long userId, String event, Object payload) {
        Set<SseEmitter> set = emitters.getOrDefault(userId, new CopyOnWriteArraySet<>());
        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event)
                        .data(payload, MediaType.APPLICATION_JSON)
                        .reconnectTime(conf.reconnectMillis()));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        var set = emitters.get(userId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) emitters.remove(userId);
        }
    }

    /** 최소 JSON 페이로드 */
    public record SimpleMsg(
            @JsonProperty("message") String message,
            @JsonProperty("type") String type,
            @JsonProperty("timestamp") String timestamp


    ) {}
}
