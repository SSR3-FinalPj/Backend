package org.example.ssj3pj.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoGeneratedEvent {
    private String eventId;   // 멱등용(원하면 AiEvent로 따로 저장)
    private Long userId;      // 실제 Users.id
    private String promptId;  // = PromptResult.pk
    private String prompt;    // 프롬프트 원문 (수정되면 덮어씀)
    private String videoId;   // 고유 영상 ID
    private String video;     // 영상 경로(EFS 내부 경로든 URL이든)
    private String status;    // PENDING/RUNNING/COMPLETED/FAILED (없으면 COMPLETED로 처리)
    private String message;   // 완료/오류 메시지
}
