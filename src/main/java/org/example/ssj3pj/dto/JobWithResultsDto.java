package org.example.ssj3pj.dto;

import lombok.*;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Job 정보와 연관된 JobResult 목록을 포함한 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWithResultsDto {

    /** Job ID */
    private Long jobId;

    /** 작업 상태 */
    private String status;

    /** 작업 목적 */
    private String platform;

    /** 위치 코드 */
    private String locationCode;

    /** 프롬프트 텍스트 */
    private String promptText;

    /** 원본 이미지 키 */
    private String sourceImageKey;

    /** 작업 생성 시간 */
    private LocalDateTime createdAt;

    /** 작업 수정 시간 */
    private LocalDateTime updatedAt;

    /** 연관된 JobResult 목록 */
    private List<JobResultDto> results;

    /** Job 엔티티로부터 DTO 생성 */
    public static JobWithResultsDto fromEntity(Job job) {
        List<JobResultDto> resultDtos = job.getResults().stream()
                .map(JobResultDto::fromEntity)
                .collect(Collectors.toList());

        return JobWithResultsDto.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .platform(job.getPlatform())
                .locationCode(job.getLocationCode())
                .promptText(job.getPromptText())
                .sourceImageKey(job.getSourceImageKey())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .results(resultDtos)
                .build();
    }
}