package org.example.ssj3pj.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.example.ssj3pj.entity.User.Users;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reddit_token",
       uniqueConstraints = @UniqueConstraint(name = "uk_reddit_token_user", columnNames = "user_id"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RedditToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // users.user_id 에 매핑 (Users 엔티티가 @Id @Column(name="user_id")라고 가정)
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "reddit_user_id", length = 40)
    private String redditUserId;

    @Column(name = "reddit_username", length = 100)
    private String redditUsername;

    @Lob
    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Lob
    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "token_type", length = 20)
    private String tokenType;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
