package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "es_environment_metadata")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EnvironmentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "es_doc_id", nullable = false, unique = true)
    private String esDocId;

    @Column(name = "location")
    private String location;

    @Column(name = "location_id")
    private String location_id;

    @Column(name = "recorded_at")
    private String recordedAt;          // 문자열로 저장되어 있다면 String 유지

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @Column(name = "source")
    private String source;// 예: "citydata"
}
