package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "kafka_message")
@Getter @Setter
public class KafkaMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private long ts;
    private Double temp;
    private Double humidity;

    @Column(columnDefinition = "TEXT")
    private String rawJson;
}
