package org.example.ssj3pj.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultNodeDto {
    private Long resultId;               // 실제 결과물 ID
    private List<ResultNodeDto> children; // 파생된 결과들
}