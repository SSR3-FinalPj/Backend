package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DashboardYTTotalStats {
    @JsonProperty("total_video_count") private long totalVideoCount;
    @JsonProperty("total_view_count")  private long totalViewCount;
    @JsonProperty("total_like_count")  private long totalLikeCount;
    @JsonProperty("total_comment_count") private long totalCommentCount;
    @JsonProperty("total_subscribe_count") private long totalSubscribeCount;
}
