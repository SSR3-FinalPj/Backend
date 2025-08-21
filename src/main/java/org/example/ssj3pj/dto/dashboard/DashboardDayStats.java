package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DashboardDayStats {
    private String date;
    @JsonProperty("view_count") private long viewCount;
    @JsonProperty("like_count") private long likeCount;
    @JsonProperty("comment_count") private long commentCount;
}
