package org.example.ssj3pj.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record CommentUpdateJobRequest(
        @NotBlank JsonNode comments,
        @NotBlank Long result_id
) {}
