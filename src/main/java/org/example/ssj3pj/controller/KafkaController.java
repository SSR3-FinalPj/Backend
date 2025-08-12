package org.example.ssj3pj.controller;

import org.example.ssj3pj.dto.kafkamessage.KafkaMessage;
import org.example.ssj3pj.kafka.KafkaProducerService;  // 다시 활성화
import org.example.ssj3pj.entity.KafkaMessageEntity;
import org.example.ssj3pj.repository.KafkaMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kafka")
public class KafkaController {
    private final KafkaProducerService producer;  // 다시 활성화
    private final KafkaMessageRepository kafkaMessageRepository;

    @PostMapping
    public String ingest(@RequestBody KafkaMessage d) {
        try {
            producer.send(d); // 다시 활성화
            System.out.println("✅ Kafka Producer로 메시지 전송: " + d);
            return "ok - message sent to kafka";
        } catch (Exception e) {
            System.err.println("❌ Kafka Producer 오류: " + e.getMessage());
            return "error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-consumer")
    public String testConsumerStatus() {
        return "Consumer 상태 확인용 - Kafka UI에서 local-consumer-group 확인해주세요";
    }
    
    @GetMapping("/test-producer")
    public String testProducer() {
        try {
            KafkaMessage testMsg = new KafkaMessage(
                "test-sensor-get", 
                System.currentTimeMillis(),  // 현재 시간을 long으로
                25.5, 
                60.0, 
                "{\"method\": \"GET\"}"
            );
            producer.send(testMsg);
            return "TEST SUCCESS: Message sent to Kafka via GET";
        } catch (Exception e) {
            return "TEST ERROR: " + e.getMessage();
        }
    }
    
    @GetMapping("/messages")
    public List<KafkaMessageEntity> getConsumedMessages() {
        System.out.println("📋 데이터베이스에서 소비된 Kafka 메시지 조회...");
        List<KafkaMessageEntity> messages = kafkaMessageRepository.findAll();
        System.out.println("✅ 총 " + messages.size() + "개의 메시지를 찾았습니다.");
        return messages;
    }
    
    @GetMapping("/messages/latest/{count}")
    public List<KafkaMessageEntity> getLatestMessages(@PathVariable int count) {
        System.out.println("📋 최신 " + count + "개 메시지 조회...");
        List<KafkaMessageEntity> allMessages = kafkaMessageRepository.findAll();
        // 간단하게 마지막 N개만 반환 (더 복잡한 쿼리는 Repository에 추가 가능)
        int size = allMessages.size();
        int start = Math.max(0, size - count);
        List<KafkaMessageEntity> latestMessages = allMessages.subList(start, size);
        System.out.println("✅ 최신 " + latestMessages.size() + "개 메시지를 반환합니다.");
        return latestMessages;
    }
}
