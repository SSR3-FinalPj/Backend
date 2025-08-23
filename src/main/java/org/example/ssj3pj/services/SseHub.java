// src/main/java/org/example/ssj3pj/sse/SseHub.java
package org.example.ssj3pj.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.config.SseConfig.SseSettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.*;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Component
@RequiredArgsConstructor
public class SseHub {

    private static final String INIT_EVENT = "init";
    private static final String VIDEO_READY_EVENT = "video-ready";
    private static final long TIMEOUT_MILLIS = 30L * 60 * 1000; // 30분
    private static final int MAX_CONN_PER_USER = 5; // 간단 남용 방지

    private final SseSettings conf;

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

    /** Kafka에서 호출: 메시지 + 타임스탬프만 발사 */
    public void notifyVideoReady(Long userId) {
        var payload = new SimpleMsg("VIDEO_READY_TEST", Instant.now().toString());
        send(userId, VIDEO_READY_EVENT, payload);
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
            @JsonProperty("timestamp") String timestamp
    ) {}
}
