package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class DashboardYTDayStats {
    private LocalDate date;
    @JsonProperty("view_count") private long viewCount;
    @JsonProperty("like_count") private long likeCount;
    @JsonProperty("subscriber_count") private long subscriberCount;
    @JsonProperty("comment_count") private long commentCount;
    @JsonProperty("video_count") private long videoCount;
}
