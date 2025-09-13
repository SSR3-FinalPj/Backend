package org.example.ssj3pj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ssj3pj.dto.dashboard.DashboardYTTotalStats;
import org.example.ssj3pj.dto.reddit.RedditContentDetailDto;
import org.example.ssj3pj.dto.youtube.UploadVideoDetailDto;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BothResultDto {
    @JsonProperty("reddit") private RedditContentDetailDto redditDetail; // 기간 전체 합계
    @JsonProperty("youtube") private YoutubeContentDetailDto youtubeDatil; // 일별 배열
    @JsonProperty("reddit_comments") private JsonNode redditComments; // 일별 배열
    @JsonProperty("youtube_comments") private JsonNode youtubeComments; // 일별 배열

}
