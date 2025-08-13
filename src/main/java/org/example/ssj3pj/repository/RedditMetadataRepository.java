package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.RedditMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedditMetadataRepository extends JpaRepository<RedditMetadata, Long> {

    // 기본 피드(최신순)
    Page<RedditMetadata> findAllByOrderByPostedAtDesc(Pageable pageable);

    // 서브레딧 필터 + 최신순
    Page<RedditMetadata> findBySubredditIgnoreCaseOrderByPostedAtDesc(String subreddit, Pageable pageable);

    // ES/Reddit 식별자로 단건 조회
    RedditMetadata findByEsDocId(String esDocId);
    RedditMetadata findByRedditId(String redditId);
}
