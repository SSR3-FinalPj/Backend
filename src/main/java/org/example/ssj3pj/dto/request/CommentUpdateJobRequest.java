package org.example.ssj3pj.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateJobRequest(
        @NotBlank String key,
        @NotBlank String locationCode,
        String prompt_text,
        @NotBlank String platform
) {}
