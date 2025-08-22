package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ssj3pj.entity.User.Users;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @Column(name = "video_path", length = 500)
    private String videoPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_path", referencedColumnName = "image_path", nullable = true) // ON DELETE SET NULL
    private Image image;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
