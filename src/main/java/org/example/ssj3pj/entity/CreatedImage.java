package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ssj3pj.entity.User.Users;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "created_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "created_images_id")
    private Long id;

    @Column(name = "created_images_key", length = 500, unique = true, nullable = false)
    private String createdImagesKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_image_id", nullable = false)
    private Image sourceImage;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";  // 기본값 PENDING

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
