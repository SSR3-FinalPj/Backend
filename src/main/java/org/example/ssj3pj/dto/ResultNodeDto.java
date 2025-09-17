package org.example.ssj3pj.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultNodeDto {
    private Long resultId;               // 실제 결과물 ID
    private Long jobId;
    private String status;
    private String type;
    private String resultKey;
    private LocalDateTime createdAt;
    private List<ResultNodeDto> children; // 파생된 결과들
}