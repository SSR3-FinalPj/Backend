package org.example.ssj3pj.dto;

import lombok.*;
import org.example.ssj3pj.entity.JobResult;

import java.time.LocalDateTime;

/**
 * JobResult 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResultDto {

    /** JobResult ID */
    private Long resultId;

    /** Job ID */
    private Long jobId;

    /** 결과 상태 */
    private String status;

    /** 결과 타입 */
    private String type;

    /** S3 결과 키 */
    private String resultKey;

    /** 프롬프트 텍스트 */
    private String promptText;

    /** 생성 시간 */
    private LocalDateTime createdAt;

    /** JobResult 엔티티로부터 DTO 생성 */
    public static JobResultDto fromEntity(JobResult jobResult) {
        return JobResultDto.builder()
                .resultId(jobResult.getId())
                .jobId(jobResult.getJob().getId())
                .status(jobResult.getStatus())
                .type(jobResult.getType())
                .resultKey(jobResult.getResultKey())
                .promptText(jobResult.getPrompt())
                .createdAt(jobResult.getCreatedAt())
                .build();
    }
}