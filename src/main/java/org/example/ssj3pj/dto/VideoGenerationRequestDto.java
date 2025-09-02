package org.example.ssj3pj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class VideoGenerationRequestDto {


    @JsonProperty("jobId")
    private Long jobId;

    @JsonProperty("img")
    private String imageKey;

    @JsonProperty("userId")
    private String userId; // FastAPI   expects string, so convert Long to String

    @JsonProperty("weather")
    private EnvironmentSummaryDto weatherData; // This will contain the ES summary

    @JsonProperty("user")
    private Map<String, Object> userData;

    private String platform;

    private boolean isClient;

    private String promptText;
}
