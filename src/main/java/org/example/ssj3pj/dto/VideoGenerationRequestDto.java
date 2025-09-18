package org.example.ssj3pj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.example.ssj3pj.entity.Prompt;

import java.util.Map;

@Data
@Builder
public class VideoGenerationRequestDto {


    @JsonProperty("jobId")
    private Long jobId;

    @JsonProperty("img")
    private String imageKey;

    @JsonProperty("mascotImg")
    private String mascotImageKey;

    @JsonProperty("weather")
    private EnvironmentSummaryDto weatherData; // This will contain the ES summary

    private String platform;

    private boolean isClient;

    @JsonProperty("user")
    private String currentPrompt;

    private String beforePrompt;

    private PromptRequest element;

    private PromptRequest sample;

    private String UUID;
}
