package org.example.ssj3pj.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "es_reddit_metadata",
        indexes = {
                @Index(name = "idx_reddit_meta_subreddit", columnList = "subreddit"),
                @Index(name = "idx_reddit_meta_posted_at", columnList = "posted_at"),
                @Index(name = "idx_reddit_meta_subreddit_posted", columnList = "subreddit, posted_at")
        }
)
public class RedditMetadata {

    // PK: 테이블의 id (bigserial 가정)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // (중요) 테이블의 id 컬럼과 동일
    private Long id;

    // 본문 메타
    @Column(name = "subreddit", nullable = false, length = 100)
    private String subreddit;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    // 레딧 원문 식별자
    @Column(name = "reddit_id", nullable = false, length = 40)
    private String redditId;          // 예: t3_xxxxx

    // ES 문서 식별자
    @Column(name = "es_doc_id", nullable = false, length = 200)
    private String esDocId;           // ES _id

    // 지표
    @Column(name = "num_comments")
    private Integer numComments;

    @Column(name = "score")
    private Integer score;

    @Column(name = "upvote_ratio")
    private Double upvoteRatio;       // numeric(4,3) ↔ Double 매핑

    // 타임스탬프
    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;         // 레딧 원문 게시 시각

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;        // 우리 쪽 수집/색인 시각
}
