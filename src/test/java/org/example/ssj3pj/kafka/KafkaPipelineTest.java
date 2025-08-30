package org.example.ssj3pj.kafka;

import org.example.ssj3pj.dto.kafkamessage.KafkaMessage;
import org.example.ssj3pj.entity.KafkaMessageEntity;
import org.example.ssj3pj.repository.KafkaMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class KafkaPipelineTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("topics.kafka.raw", () -> "test-topic");
        // 테스트 실행 시에는 H2 DB를 사용하도록 설정
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "password");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    }

    @Autowired
    private KafkaProducerService producerService;

    @Autowired
    private KafkaMessageRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void testKafkaPipeline() {
        System.out.println("\n=== Kafka Pipeline Test 시작 ===");

        // given: 테스트 메시지 생성
        String source = "test-device-1";
        long ts = System.currentTimeMillis();
        double temp = 25.5;
        double humidity = 60.2;
        String rawJson = String.format("{\"source\":\"%s\",\"ts\":%d,\"temp\":%.1f,\"humidity\":%.1f}", source, ts, temp, humidity);
        KafkaMessage message = new KafkaMessage(source, ts, temp, humidity, rawJson);

        System.out.println("📤 전송할 메시지 생성:");
        System.out.println("   Source: " + source);
        System.out.println("   Timestamp: " + ts);
        System.out.println("   Temperature: " + temp + "°C");
        System.out.println("   Humidity: " + humidity + "%");
        System.out.println("   Raw JSON: " + rawJson);
        System.out.println();

        // when: Kafka Producer를 통해 메시지 전송
        System.out.println("🚀 Kafka Producer를 통해 메시지 전송 중...");
        producerService.send(message);
        System.out.println("✅ 메시지 전송 완료!");
        System.out.println();

        // then: Consumer가 메시지를 처리하여 DB에 저장할 때까지 대기 (최대 10초)
        System.out.println("⏳ Consumer가 메시지를 처리하여 DB에 저장할 때까지 대기 중... (최대 10초)");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<KafkaMessageEntity> savedMessages = repository.findAll();
            assertThat(savedMessages).hasSize(1);

            KafkaMessageEntity savedEntity = savedMessages.get(0);
            System.out.println("\n📥 Consumer가 메시지를 수신하고 DB에 저장 완료!");
            System.out.println("💾 저장된 데이터:");
            System.out.println("   ID: " + savedEntity.getId());
            System.out.println("   Source: " + savedEntity.getSource());
            System.out.println("   Timestamp: " + savedEntity.getTs());
            System.out.println("   Temperature: " + savedEntity.getTemp() + "°C");
            System.out.println("   Humidity: " + savedEntity.getHumidity() + "%");
            System.out.println("   Raw JSON: " + savedEntity.getRawJson());
            System.out.println();

            assertThat(savedEntity.getSource()).isEqualTo(source);
            assertThat(savedEntity.getTs()).isEqualTo(ts);
            assertThat(savedEntity.getTemp()).isEqualTo(temp);
            assertThat(savedEntity.getHumidity()).isEqualTo(humidity);
            assertThat(savedEntity.getRawJson()).isEqualTo(rawJson);
        });

        System.out.println("🎉 전체 파이프라인 테스트 성공!");
        System.out.println("   Producer → Kafka → Consumer → Database 흐름 확인 완료");
        System.out.println("=== Kafka Pipeline Test 종료 ===\n");
    }

    @Test
    void testMultipleMessages() {
        System.out.println("\n=== 다중 메시지 파이프라인 테스트 시작 ===");

        // given: 3개의 다른 디바이스에서 온 메시지
        long baseTs = System.currentTimeMillis();

        KafkaMessage msg1 = new KafkaMessage("sensor-room-1", baseTs, 22.5, 55.0,
                "{\"source\":\"sensor-room-1\",\"ts\":" + baseTs + ",\"temp\":22.5,\"humidity\":55.0}");
        KafkaMessage msg2 = new KafkaMessage("sensor-room-2", baseTs + 1000, 26.8, 62.3,
                "{\"source\":\"sensor-room-2\",\"ts\":" + (baseTs + 1000) + ",\"temp\":26.8,\"humidity\":62.3}");
        KafkaMessage msg3 = new KafkaMessage("sensor-outdoor", baseTs + 2000, 18.2, 78.5,
                "{\"source\":\"sensor-outdoor\",\"ts\":" + (baseTs + 2000) + ",\"temp\":18.2,\"humidity\":78.5}");

        System.out.println("📤 3개의 센서 메시지 전송:");

        // when: 3개 메시지 순차 전송
        System.out.println("1. Room-1 Sensor: " + msg1.temp() + "°C, " + msg1.humidity() + "%");
        producerService.send(msg1);

        System.out.println("2. Room-2 Sensor: " + msg2.temp() + "°C, " + msg2.humidity() + "%");
        producerService.send(msg2);

        System.out.println("3. Outdoor Sensor: " + msg3.temp() + "°C, " + msg3.humidity() + "%");
        producerService.send(msg3);

        System.out.println("✅ 모든 메시지 전송 완료!\n");

        // then: 3개 메시지 모두 DB에 저장될 때까지 대기
        System.out.println("⏳ 모든 메시지가 처리될 때까진 대기 중...");
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<KafkaMessageEntity> savedMessages = repository.findAll();
            assertThat(savedMessages).hasSize(3);

            System.out.println("\n📊 저장된 데이터 요약:");
            for (int i = 0; i < savedMessages.size(); i++) {
                KafkaMessageEntity entity = savedMessages.get(i);
                System.out.println("   " + (i+1) + ". [" + entity.getSource() + "] "
                        + entity.getTemp() + "°C, " + entity.getHumidity() + "% (ID: " + entity.getId() + ")");
            }
        });

        System.out.println("\n🎉 다중 메시지 파이프라인 테스트 성공!");
        System.out.println("   다양한 센서에서 온 데이터가 모두 정상적으로 처리되었습니다.");
        System.out.println("=== 다중 메시지 파이프라인 테스트 종료 ===\n");
    }
}