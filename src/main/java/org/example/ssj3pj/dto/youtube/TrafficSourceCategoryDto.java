package org.example.ssj3pj.dto.youtube;

import lombok.Builder;
import lombok.Getter;

/** 트래픽 소스 카테고리 요약 */
@Getter
@Builder
public class TrafficSourceCategoryDto {
    private final String categoryName;       // "검색 (Search)", "추천/탐색 (Discovery)" 등
    private final String categoryCode;       // "SEARCH", "DISCOVERY" 등  
    private final long totalViews;          // 해당 카테고리 총 조회수
}
