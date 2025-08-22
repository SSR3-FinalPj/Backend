package org.example.ssj3pj.dto;

import lombok.Builder;
import lombok.Getter;

/** 일자 내 인구통계 세부 점 (연령/성별/비율) */
@Getter
@Builder
public class DemographicPoint {
    private final String ageGroup;
    private final String gender;
    private final double viewerPercentage;
}
