package org.example.ssj3pj.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class CommentAnalysisRequest {
    private Map<String, Object> youtube;
    private Map<String, Object> reddit;
}
