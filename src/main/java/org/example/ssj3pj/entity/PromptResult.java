package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ssj3pj.entity.User.Users;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "prompt_results",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_prompt_results_video_id", columnNames = "video_id")
    },
    indexes = {
        @Index(name = "idx_prompt_results_user", columnList = "user_id"),
        @Index(name = "idx_prompt_results_video_id", columnList = "video_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromptResult {

    /** 프롬프트 하나당 한 행 → 1:1 보장 */
    @Id
    @Column(name = "prompt_id", length = 100)
    private String promptId;                // e.g. "prm_xxx" (외부와 공유되는 키)

    /** 실제 유저의 ID를 FK로 연결 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;                     // users.id (실제 유저 PK)

    /** 프롬프트 원문 */
    @Lob
    @Column(name = "prompt_text", nullable = false)
    private String promptText;

    /** 생성된 영상 식별자/경로 (1:1이므로 이 테이블에 보관) */
    @Column(name = "video_id", nullable = false, length = 100, unique = true)
    private String videoId;                 // e.g. "vid_xxx"

    @Column(name = "video_path", nullable = false, length = 2048)
    private String videoPath;               // s3://... 또는 https://...

    /** 상태/메시지(선택) */
    @Column(name = "status", nullable = false)
    private String status;                  // PENDING/RUNNING/COMPLETED/FAILED

    @Column(name = "message")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
