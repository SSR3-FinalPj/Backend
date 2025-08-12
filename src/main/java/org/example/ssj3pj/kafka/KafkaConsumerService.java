package org.example.ssj3pj.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.kafkamessage.KafkaMessage;
import org.example.ssj3pj.services.KafkaMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final KafkaMessageService kafkaMessageService;

    @Value("${spring.topics.kafka.raw}")
    private String topicName;

    @KafkaListener(topics = "${spring.topics.kafka.raw}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(
            KafkaMessage message, 
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header("kafka_receivedPartitionId") int partition,
            @Header("kafka_offset") long offset
    ) {
        try {
            log.info("\n🔔 Kafka Consumer 메시지 수신:");
            log.info("   Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
            log.info("   Message: {}", message);
            
            // 데이터베이스에 저장
            kafkaMessageService.save(message);
            log.info("✅ 메시지를 데이터베이스에 저장 완료!");
            
            // 수동 커밋
            acknowledgment.acknowledge();
            log.info("✅ 메시지 처리 및 커밋 완료!\n");
            
        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 커밋하지 않음 (재처리를 위해)
        }
    }
}
