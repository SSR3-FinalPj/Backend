package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.YoutubeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface YoutubeMetadataRepository extends JpaRepository<YoutubeMetadata, Long> {
    Optional<YoutubeMetadata> findByYoutubeId(String youtubeId);

    List<YoutubeMetadata> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);
}
