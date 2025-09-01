package org.example.ssj3pj.kafka;

import org.example.ssj3pj.entity.KafkaMessageEntity;
import org.example.ssj3pj.repository.KafkaMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test") // 테스트 프로필 사용
class DatabaseVerificationTest {

    @MockBean
    private S3Client s3Client;

    @MockBean
    private S3Presigner s3Presigner;

    @Autowired
    private KafkaMessageRepository repository;

    @Test
    void checkDatabaseContents() {
        System.out.println("\n=== 데이터베이스 내용 확인 ===");
        
        List<KafkaMessageEntity> allMessages = repository.findAll();
        
        if (allMessages.isEmpty()) {
            System.out.println("📭 데이터베이스에 저장된 메시지가 없습니다.");
        } else {
            System.out.println("📊 총 " + allMessages.size() + "개의 메시지가 저장되어 있습니다:");
            System.out.println();
            
            for (int i = 0; i < allMessages.size(); i++) {
                KafkaMessageEntity msg = allMessages.get(i);
                System.out.println("🔍 메시지 " + (i + 1) + ":");
                System.out.println("   ID: " + msg.getId());
                System.out.println("   Source: " + msg.getSource());
                System.out.println("   Timestamp: " + msg.getTs());
                System.out.println("   Temperature: " + msg.getTemp() + "°C");
                System.out.println("   Humidity: " + msg.getHumidity() + "%");
                System.out.println("   Raw JSON: " + msg.getRawJson());
                System.out.println();
            }
        }
        
        System.out.println("=== 데이터베이스 확인 완료 ===\n");
    }

    @Test
    void checkRecentMessages() {
        System.out.println("\n=== 최근 메시지 확인 (최대 5개) ===");
        
        List<KafkaMessageEntity> allMessages = repository.findAll();
        
        // 타임스탬프 기준으로 정렬
        List<KafkaMessageEntity> recentMessages = allMessages.stream()
            .sorted((a, b) -> Long.compare(b.getTs(), a.getTs())) // 최신순
            .limit(5)
            .toList();
        
        if (recentMessages.isEmpty()) {
            System.out.println("📭 최근 메시지가 없습니다.");
        } else {
            System.out.println("📈 최근 " + recentMessages.size() + "개 메시지:");
            System.out.println();
            
            for (KafkaMessageEntity msg : recentMessages) {
                System.out.printf("🕐 %s | %s | %.1f°C, %.1f%% | ID: %d%n", 
                    new java.util.Date(msg.getTs()),
                    msg.getSource(),
                    msg.getTemp(),
                    msg.getHumidity(),
                    msg.getId());
            }
        }
        
        System.out.println("\n=== 최근 메시지 확인 완료 ===\n");
    }
}
