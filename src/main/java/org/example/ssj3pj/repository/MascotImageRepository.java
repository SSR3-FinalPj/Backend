package org.example.ssj3pj.repository;
import org.example.ssj3pj.entity.MascotImage;
import org.example.ssj3pj.entity.User.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MascotImageRepository extends JpaRepository<MascotImage, Long> {
    Optional<MascotImage> findByRegionCode(String regionCode);
}
