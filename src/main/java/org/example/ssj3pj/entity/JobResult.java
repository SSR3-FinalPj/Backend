package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "job_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING"; // pending, processing, completed, failed

    @Column(name = "type", length = 20, nullable = false)
    private String type; // image, video 등

    @Column(name = "result_key", length = 500, nullable = false, unique = true)
    private String resultKey; // S3 key

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
