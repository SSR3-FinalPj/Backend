package org.example.ssj3pj.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "es_environment_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "es_doc_id", nullable = false, unique = true)
    private String esDocId;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    private String location;

    private String source;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt = LocalDateTime.now();
}
