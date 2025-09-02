package org.example.ssj3pj.dto.reddit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ssj3pj.dto.youtube.VideoStatisticsDto;

/**
 * 비디오 아이템 DTO (비디오 목록에서 사용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostItemDto {
    /**
     * YouTube 비디오 ID
     */
    private String postId;
    
    /**
     * 비디오 제목
     */
    private String title;
    
    /**
     * 썸네일 URL (고품질)
     */
    private String thumbnail;
    
    /**
     * 게시 일시 (ISO 8601 형식)
     */
    private String publishedAt;
    
    /**
     * YouTube 비디오 URL
     */
    private String url;
    
    /**
     * 비디오 통계 정보
     */
    private PostStatisticsDto statistics;
}
