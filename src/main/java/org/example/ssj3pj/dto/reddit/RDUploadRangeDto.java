package org.example.ssj3pj.dto.reddit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ssj3pj.dto.dashboard.DashboardRDTotalStats;
import org.example.ssj3pj.dto.dashboard.DashboardYTTotalStats;
import org.example.ssj3pj.dto.youtube.UploadVideoDetailDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RDUploadRangeDto {
    @JsonProperty("total") private DashboardRDTotalStats total; // 기간 전체 합계
    @JsonProperty("posts") private List<RedditContentDetailDto> posts; // 일별 배열

}
