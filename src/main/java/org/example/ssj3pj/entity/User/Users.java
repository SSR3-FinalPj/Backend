package org.example.ssj3pj.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users") // 테이블명 명확하게 지정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private GoogleToken googleToken;

}
