package org.example.ssj3pj.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * YouTube API 페이지네이션 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfoDto {
    /**
     * 현재 페이지에서 반환된 결과 수
     */
    private Integer resultsPerPage;
    
    /**
     * 전체 결과 수 (추정치)
     */
    private Integer totalResults;
}
