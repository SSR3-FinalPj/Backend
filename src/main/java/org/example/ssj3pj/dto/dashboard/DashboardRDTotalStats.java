package org.example.ssj3pj.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DashboardRDTotalStats {
    @JsonProperty("total_post_count") private long totalPostCount;
    @JsonProperty("total_upvote_ratio")  private double totalUpvoteRatio;
    @JsonProperty("total_upvote_count")  private long totalUpvoteCount;
    @JsonProperty("total_comment_count") private long totalCommentCount;
}
