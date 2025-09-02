package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class DashboardRDDayStats {
    private LocalDate date;
    @JsonProperty("post_count") private long postCount;
    @JsonProperty("upvote_count") private long upvoteCount;
    @JsonProperty("upvote_ratio") private Double upvoteRatio;
    @JsonProperty("comment_count") private long commentCount;
}
