package org.example.ssj3pj.entity;
import jakarta.persistence.*;
import lombok.*;
import org.example.ssj3pj.entity.User.GoogleToken;

@Entity
@Table(name = "mascot_images") // 테이블명 명확하게 지정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MascotImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mascot_id")
    private Long id;

    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Column(name = "mascot_image_key", nullable = false)
    private String mascotImageKey;
}
