package org.example.ssj3pj.kafka;

import org.example.ssj3pj.dto.kafkamessage.KafkaMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.topics.kafka.raw}")
    private String topic;

    public void send(KafkaMessage msg) {
        // 키를 source로 주면 동일 source가 같은 파티션으로 가서 순서/집계 유리
        kafkaTemplate.send(topic, msg.source(), msg);
    }
}
