package org.example.ssj3pj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ssj3pj.dto.reddit.RedditContentDetailDto;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BothUploadDto {
    private Long resultId;
    private String title;
    private String uploadedAt;
    private String youtube;
    private String reddit;
}
