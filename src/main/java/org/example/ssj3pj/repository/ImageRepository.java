package org.example.ssj3pj.repository;

import java.util.Optional;

import org.example.ssj3pj.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
    Optional<Image> findTopByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<Image> findByImageKey(String imageKey);
}
