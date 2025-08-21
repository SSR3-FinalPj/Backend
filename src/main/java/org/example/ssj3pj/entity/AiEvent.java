package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ssj3pj.entity.User.Users;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ai_events",
       indexes = {
         @Index(name = "idx_ai_events_user", columnList = "user_id"),
         @Index(name = "idx_ai_events_prompt", columnList = "promptId"),
         @Index(name = "idx_ai_events_video",  columnList = "videoId")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private String eventId;          // 멱등 키 (UUID 등)

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)   // ✅ 실제 Users.id FK
    private Users user;

    @Column(nullable = false)
    private String promptId;         // 예: "prm_xxx" (문자열)

    @Lob
    @Column(nullable = false)
    private String prompt;           // 프롬프트 원문

    @Column(nullable = false)
    private String videoId;          // 예: "vid_xxx"

    @Column(nullable = false, length = 2048)
    private String video;            // 영상 경로/URL (s3/http 등)

    private String message;          // 완료/상태 메시지

    @Lob
    @Column(nullable = false)
    private String payloadRaw;       // 이벤트 원문 전체(JSON)

    @CreationTimestamp
    private Instant receivedAt;
}
