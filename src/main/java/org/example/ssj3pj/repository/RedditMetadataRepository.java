package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.RedditMetadata;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RedditMetadataRepository extends JpaRepository<RedditMetadata, Long> {

    Optional<RedditMetadata> findFirstByUserOrderByIndexedAtDesc(Users user);
    Optional<RedditMetadata> findFirstByUserAndIndexedAtBetweenOrderByIndexedAtDesc(
            Users user,
            LocalDateTime start,
            LocalDateTime  end
    );
    Optional<RedditMetadata> findFirstByUserAndChannelIdOrderByIndexedAtDesc(Users user, String channelId);
}
