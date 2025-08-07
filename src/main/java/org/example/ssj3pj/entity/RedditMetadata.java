package org.example.ssj3pj.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "es_reddit_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "es_doc_id", nullable = false, unique = true)
    private String esDocId;

    @Column(name = "reddit_id", nullable = false, unique = true)
    private String redditId;

    private String title;

    private String subreddit;

    private Integer score;

    private Integer numComments;

    private Float upvoteRatio;

    private LocalDateTime postedAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt = LocalDateTime.now();
}
