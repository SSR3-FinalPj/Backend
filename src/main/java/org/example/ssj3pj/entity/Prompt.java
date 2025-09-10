package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prompt {

    @Id
    @Column(name = "job_id")
    private Long jobId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId   // PK를 그대로 FK로 사용
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "action", columnDefinition = "TEXT")
    private String action;

    @Column(name = "style", columnDefinition = "TEXT")
    private String style;

    @Column(name = "camera_positioning", columnDefinition = "TEXT")
    private String cameraPositioning;

    @Column(name = "composition", columnDefinition = "TEXT")
    private String composition;

    @Column(name = "focus_and_lens", columnDefinition = "TEXT")
    private String focusAndLens;

    @Column(name = "ambiance", columnDefinition = "TEXT")
    private String ambiance;
}