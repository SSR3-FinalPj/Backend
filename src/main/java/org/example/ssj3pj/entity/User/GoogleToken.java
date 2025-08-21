package org.example.ssj3pj.entity.User;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "google_token")
public class GoogleToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    // 🔁 재동의 받기 전엔 null일 수 있어 null 허용 권장
    @Column(nullable = true, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(nullable = false)
    private Instant expiresAt;

    // ✅ 추가: YouTube 채널 ID (고정 식별자)
    @Column(length = 64)
    private String youtubeChannelId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
