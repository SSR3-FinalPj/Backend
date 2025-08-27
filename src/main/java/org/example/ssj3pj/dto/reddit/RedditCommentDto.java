package org.example.ssj3pj.dto.reddit;

import lombok.Builder;
import lombok.Data;

// 댓글

@Data
@Builder
public class RedditCommentDto {
    private String id;
    private String author;
    private String body;
    private Integer score;
    private String createdAt; // ISO-8601
    private Integer depth;
    private String parentId;
    private Integer numReplies;
}
