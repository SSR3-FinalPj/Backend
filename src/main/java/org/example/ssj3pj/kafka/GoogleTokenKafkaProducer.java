package org.example.ssj3pj.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.GoogleAccessTokenEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.topics.kafka.googleAccessToken:}")
    private String topic;

    public void publish(GoogleAccessTokenEvent evt) {
        kafkaTemplate.send(topic, String.valueOf(evt.getUserId()), evt)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] send failed userId={} event={}", evt.getUserId(), evt.getEvent(), ex);
                } else {
                    log.info("[KAFKA] sent userId={} event={} partition={} offset={}",
                        evt.getUserId(), evt.getEvent(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
