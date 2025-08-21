package org.example.ssj3pj.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.EnvironmentMetadata;
import org.example.ssj3pj.services.EnvironmentDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.example.ssj3pj.services.EnvironmentQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final EnvironmentQueryService environmentDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.topics.kafka.raw}")
    private String topicName;

    // 배치 처리를 위한 데이터 저장소
    private final List<EnvironmentMetadata> batchData = new ArrayList<>();
    private final AtomicInteger messageCount = new AtomicInteger(0);
    private static final int BATCH_SIZE = 120; // 서울 실시간 데이터 총량
    private static final String COMPLETION_SIGNAL = "City data to ES Complete";
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @KafkaListener(topics = "${spring.topics.kafka.raw}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(
            String jsonMessage,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header("kafka_receivedPartitionId") int partition,
            @Header("kafka_offset") long offset
    ) {
        try {
            log.info("\n🔔 Kafka Consumer 메시지 수신:");
            log.info("   Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
            log.info("   메시지 길이: {} characters", jsonMessage.length());
            log.info("   메시지 내용: '{}'", jsonMessage);
            log.info("   완료 신호와 비교: '{}' vs '{}'", jsonMessage.trim(), COMPLETION_SIGNAL);
            log.info("   완료 신호 매칭: {}", COMPLETION_SIGNAL.equals(jsonMessage.trim()));

            // 완료 신호 체크 - JSON 형태로 변경
            if (isCompletionMessage(jsonMessage)) {
                log.info("✅ 완료 신호 감지! 배치 처리 시작...");
                handleBatchCompletion(acknowledgment);
                return;
            }

            // 일반 데이터 처리
            EnvironmentMetadata environmentMetadata = parseEnvironmentMessage(jsonMessage);

            if (environmentMetadata != null) {
                synchronized (batchData) {
                    batchData.add(environmentMetadata);
                    int currentCount = messageCount.incrementAndGet();

                    log.info("   배치 데이터 추가: {}/{} (location: {}, es_doc_id: {}, source: {})",
                        currentCount, BATCH_SIZE,
                        environmentMetadata.getLocation(), environmentMetadata.getEsDocId(), environmentMetadata.getSource());

                    // 배치 사이즈 도달 시 경고 (완료 신호 대기)
                    if (currentCount >= BATCH_SIZE) {
                        log.warn("⚠️ 배치 사이즈({})에 도달했습니다. 완료 신호 대기 중...", BATCH_SIZE);
                    }
                }
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 완료 신호 메시지인지 확인
     */
    private boolean isCompletionMessage(String jsonMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonMessage);

            // message 필드가 있고 "City data to ES Complete"를 포함하면 완료 신호
            if (jsonNode.has("message")) {
                String message = jsonNode.get("message").asText();
                boolean isCompletion = COMPLETION_SIGNAL.equals(message);
                log.info("🔍 완료 신호 검사: '{}' == '{}' => {}", message, COMPLETION_SIGNAL, isCompletion);
                return isCompletion;
            }

            return false;

        } catch (Exception e) {
            log.warn("🙈 완료 신호 검사 중 JSON 파싱 오류: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JSON 메시지를 EnvironmentMetadata로 파싱
     */
    private EnvironmentMetadata parseEnvironmentMessage(String jsonMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonMessage);

            // 완료 신호 체크
            if (jsonNode.has("message") && jsonNode.get("message").asText().contains("Complete")) {
                return null; // 완료 신호는 null 반환
            }

            // 필수 필드 검증
            if (!jsonNode.has("es_doc_id") || !jsonNode.has("location") || !jsonNode.has("source")) {
                log.warn("❌ 필수 필드 누락: {}", jsonMessage);
                return null;
            }

            EnvironmentMetadata metadata = new EnvironmentMetadata();

            // 필수 필드 설정
            metadata.setEsDocId(jsonNode.get("es_doc_id").asText());
            metadata.setLocation(jsonNode.get("location").asText());
            metadata.setSource(jsonNode.get("source").asText());

            // recorded_at 설정
            if (jsonNode.has("recorded_at")) {
                metadata.setRecordedAt(jsonNode.get("recorded_at").asText());
            }

            // indexed_at 설정 - 현재 시간으로 설정
            metadata.setIndexedAt(LocalDateTime.now());

            return metadata;

        } catch (Exception e) {
            log.error("❌ JSON 파싱 오류: {}, 메시지: {}", e.getMessage(), jsonMessage);
            return null;
        }
    }

    private void handleBatchCompletion(Acknowledgment acknowledgment) {
        synchronized (batchData) {
            int dataSize = batchData.size();
            log.info("\n🎯 배치 완료 신호 수신! 총 {}개 데이터 일괄 저장 시작...", dataSize);

            if (dataSize == 0) {
                log.warn("⚠️ 저장할 데이터가 없습니다.");
                acknowledgment.acknowledge();
                return;
            }

            try {
                // 배치로 EnvironmentMetadata를 데이터베이스에 저장
                environmentDataService.saveAll(new ArrayList<>(batchData));

                log.info("✅ 배치 저장 완료! {}개 데이터를 데이터베이스에 저장했습니다.", dataSize);
                log.info("   - 예상 배치 크기: {}", BATCH_SIZE);
                log.info("   - 실제 저장 크기: {}", dataSize);

                // 배치 데이터 초기화
                batchData.clear();
                messageCount.set(0);

                acknowledgment.acknowledge();
                log.info("✅ 배치 처리 및 커밋 완료!\n");

            } catch (Exception e) {
                log.error("❌ 배치 저장 중 오류 발생: {}", e.getMessage(), e);
                // 오류 발생 시 커밋하지 않음
            }
        }
    }
}
