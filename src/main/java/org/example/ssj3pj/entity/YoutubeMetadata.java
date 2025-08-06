package org.example.ssj3pj.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "es_youtube_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "es_doc_id", nullable = false, unique = true)
    private String esDocId;

    @Column(name = "youtube_id", nullable = false, unique = true)
    private String youtubeId;

    private String title;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private LocalDateTime publishedAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt = LocalDateTime.now();
}