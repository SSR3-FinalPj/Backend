package org.example.ssj3pj.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 단일 영상 상세 조회 API 응답 DTO
 * GET /api/youtube/video/{videoId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailDto {
    /**
     * 비디오 제목
     */
    private String title;
    
    /**
     * 썸네일 URL (최고 품질)
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
    private VideoStatisticsDto statistics;
}
