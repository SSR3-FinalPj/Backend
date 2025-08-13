package org.example.ssj3pj.controller;

import org.example.ssj3pj.dto.kafkamessage.KafkaMessage;
import org.example.ssj3pj.kafka.KafkaProducerService;
// import org.example.ssj3pj.entity.KafkaMessageEntity;  // 주석처리
import org.example.ssj3pj.entity.EnvironmentMetadata;
// import org.example.ssj3pj.repository.KafkaMessageRepository;  // 주석처리
import org.example.ssj3pj.repository.EnvironmentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kafka")
public class KafkaController {
    private final KafkaProducerService producer;
    private final EnvironmentMetadataRepository environmentMetadataRepository;

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

    
    /**
     * 새로운 EnvironmentMetadata 조회 엔드포인트
     */
    @GetMapping("/environment-metadata")
    public List<EnvironmentMetadata> getEnvironmentMetadata() {
        System.out.println("🌍 EnvironmentMetadata 조회...");
        try {
            List<EnvironmentMetadata> metadata = environmentMetadataRepository.findAll();
            System.out.println("✅ 총 " + metadata.size() + "개의 EnvironmentMetadata를 찾았습니다.");
            if (!metadata.isEmpty()) {
                System.out.println("📄 최근 데이터 샘플: location=" + metadata.get(metadata.size()-1).getLocation() 
                    + ", es_doc_id=" + metadata.get(metadata.size()-1).getEsDocId());
            }
            return metadata;
        } catch (Exception e) {
            System.err.println("❌ EnvironmentMetadata 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

}
