package org.example.ssj3pj.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.EnvironmentMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
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
            // 완료 신호 체크
            if (isCompletionMessage(jsonMessage)) {
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
                            environmentMetadata.getLocation(),
                            environmentMetadata.getEsDocId(),
                            environmentMetadata.getSource());

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
            metadata.setEsDocId(jsonNode.get("es_doc_id").asText());
            metadata.setLocation(jsonNode.get("location").asText());
            metadata.setSource(jsonNode.get("source").asText());

            if (jsonNode.has("recorded_at")) {
                metadata.setRecordedAt(jsonNode.get("recorded_at").asText());
            }

            metadata.setIndexedAt(LocalDateTime.now());

            return metadata;

        } catch (Exception e) {
            log.error("❌ JSON 파싱 오류: {}, 메시지: {}", e.getMessage(), jsonMessage);
            return null;
        }
    }

    /**
     * 배치 완료 처리 (부분 저장 방식)
     */
    private void handleBatchCompletion(Acknowledgment acknowledgment) {
        synchronized (batchData) {
            int dataSize = batchData.size();
            log.info("\n🎯 배치 완료 신호 수신! 총 {}개 데이터 저장 시도...", dataSize);

            if (dataSize == 0) {
                log.warn("⚠️ 저장할 데이터가 없습니다.");
                acknowledgment.acknowledge();
                return;
            }

            int successCount = 0;
            int failCount = 0;

            List<EnvironmentMetadata> toSave = new ArrayList<>(batchData);

            for (EnvironmentMetadata data : toSave) {
                try {
                    environmentDataService.save(data); // 단건 저장
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ 단건 저장 실패 (es_doc_id={}): {}", data.getEsDocId(), e.getMessage());
                }
            }

            log.info("✅ 배치 저장 완료! 성공 {}건, 실패 {}건", successCount, failCount);

            // 배치 데이터 초기화 및 오프셋 커밋
            batchData.clear();
            messageCount.set(0);
            acknowledgment.acknowledge();
            log.info("✅ 배치 처리 및 커밋 완료!\n");
        }
    }
}
