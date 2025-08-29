package org.example.ssj3pj.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.example.ssj3pj.entity.User.Users;

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
    @Column(name = "ytdata_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "es_doc_id", nullable = false, unique = true)
    private String esDocId;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "indexed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime indexedAt;
}
