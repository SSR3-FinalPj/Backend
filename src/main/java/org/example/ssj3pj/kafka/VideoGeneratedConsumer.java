package org.example.ssj3pj.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.services.PromptResultService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoGeneratedConsumer {

    private final PromptResultService promptResultService;

    @KafkaListener(
        topics = "video-callback",
        groupId = "video-generated-consumers"
        // , concurrency = "3" // 필요시 병렬 처리
    )
    public void onMessage(String rawJson) {
        // 멱등 필요하면: 별도 AiEvent 테이블로 eventId PK 저장 후 중복 스킵
        promptResultService.upsertFromRawEvent(rawJson);
        log.info("[KAFKA] 여기를 봐주세요!! processed video.generated event");
    }
}
