package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class DashboardRDRangeStats {
    @JsonProperty("total") private DashboardRDTotalStats total; // 기간 전체 합계
    @JsonProperty("daily") private List<DashboardRDDayStats> daily; // 일별 배열
}