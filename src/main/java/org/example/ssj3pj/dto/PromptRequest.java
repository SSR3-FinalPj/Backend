package org.example.ssj3pj.dto;

import jakarta.persistence.Column;
import lombok.*;

/** 기간 공통 요청 DTO (YYYY-MM-DD) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptRequest {
    private String subject;

    private String action;

    private String style;

    private String cameraPositioning;

    private String composition;

    private String focusAndLens;

    private String ambiance;
}
