package org.example.ssj3pj.dto.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ssj3pj.dto.dashboard.DashboardYTTotalStats;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRangeDto {
    @JsonProperty("total") private DashboardYTTotalStats total; // 기간 전체 합계
    @JsonProperty("videos") private List<UploadVideoDetailDto> videos; // 일별 배열

}
