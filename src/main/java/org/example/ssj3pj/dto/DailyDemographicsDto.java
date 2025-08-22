package org.example.ssj3pj.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 일자 단위 인구통계 묶음 */
@Getter
@Builder
public class DailyDemographicsDto {
    private final String date;                    // YYYY-MM-DD
    private final List<DemographicPoint> demographics;
}
