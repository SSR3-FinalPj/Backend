package org.example.ssj3pj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class VideoGenerationRequestDto {

    @JsonProperty("img")
    private String imageKey;

    @JsonProperty("userId")
    private String userId; // FastAPI   expects string, so convert Long to String

    @JsonProperty("weather")
    private EnvironmentSummaryDto weatherData; // This will contain the ES summary

    // These might be empty objects, include them if FastAPI expects them explicitly
    @JsonProperty("youtube")
    private Map<String, Object> youtubeData;

    @JsonProperty("reddit")
    private Map<String, Object> redditData;

    @JsonProperty("user")
    private Map<String, Object> userData;

}
