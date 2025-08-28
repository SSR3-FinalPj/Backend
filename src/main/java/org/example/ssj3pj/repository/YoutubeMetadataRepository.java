package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface YoutubeMetadataRepository extends JpaRepository<YoutubeMetadata, Long> {

    Optional<YoutubeMetadata> findFirstByUserOrderByIndexedAtDesc(Users user);
}
