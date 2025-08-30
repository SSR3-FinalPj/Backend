package org.example.ssj3pj.dto.response;

import lombok.*;

/**
 * YouTube 업로드 결과 응답 데이터
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeUploadResultDto {

    /** 업로드 성공 여부 */
    private boolean success;

    /** YouTube 비디오 ID */
    private String videoId;

    /** YouTube 비디오 URL */
    private String videoUrl;

    /** 업로드된 비디오 제목 */
    private String title;

    /** 오류 메시지 (실패 시) */
    private String errorMessage;

    /** Job ID */
    private Long jobId;

    /** JobResult ID */
    private Long resultId;
}