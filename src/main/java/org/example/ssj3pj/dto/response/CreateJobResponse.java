package org.example.ssj3pj.dto.response;

public record CreateJobResponse(
        Long jobId,
        String status,
        String sourceImageKey,
        String promptText
) {}
