package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class DashboardYTRangeStats {
    @JsonProperty("total") private DashboardYTTotalStats total; // 기간 전체 합계
    @JsonProperty("daily") private List<DashboardYTDayStats> daily; // 일별 배열
}