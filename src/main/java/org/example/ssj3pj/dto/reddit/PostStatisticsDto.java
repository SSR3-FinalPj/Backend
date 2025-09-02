package org.example.ssj3pj.dto.reddit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비디오 통계 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostStatisticsDto {
    /**
     * 조회수
     */
    private Long viewCount;
    
    /**
     * 좋아요 수
     */
    private Long likeCount;
    
    /**
     * 댓글 수
     */
    private Long commentCount;
}
