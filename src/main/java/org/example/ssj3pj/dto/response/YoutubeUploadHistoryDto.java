package org.example.ssj3pj.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * YouTube 업로드 이력 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeUploadHistoryDto {

    /** 업로드 기록 ID */
    private Long uploadId;

    /** Job ID */
    private Long jobId;

    /** JobResult ID */
    private Long resultId;

    /** YouTube 비디오 ID */
    private String youtubeVideoId;

    /** 비디오 제목 */
    private String title;

    /** 비디오 URL */
    private String videoUrl;

    /** 업로드 상태 */
    private String status;

    /** 업로드 시간 */
    private LocalDateTime uploadedAt;
}