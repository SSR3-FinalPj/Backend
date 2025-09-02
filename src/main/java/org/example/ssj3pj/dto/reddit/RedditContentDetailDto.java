package org.example.ssj3pj.dto.reddit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * YouTube 콘텐츠 상세 정보 조회 API 응답 DTO
 * GET /api/contents/{video_id} 엔드포인트의 응답 형식
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedditContentDetailDto {
    
    @JsonProperty("post_id")
    private String postId;
    
    @JsonProperty("upload_date")
    private String uploadDate;  // ISO 8601 형식 문자열 (예 : 2025-08-05T00:00:27Z)
    
    @JsonProperty("text")
    private String text;  // High 퀄리티 썸네일 URL (hqdefault)
    
    private String title;
    
    @JsonProperty("view_count")
    private Integer viewCount;
    
    @JsonProperty("comment_count")
    private Integer commentCount;
    
    @JsonProperty("like_count")
    private Integer likeCount;
}
