package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class DashboardRangeStats {
    @JsonProperty("total") private DashboardTotalStats total; // 기간 전체 합계
    @JsonProperty("daily") private List<DashboardDayStats> daily; // 일별 배열
}