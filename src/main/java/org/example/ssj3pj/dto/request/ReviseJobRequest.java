package org.example.ssj3pj.dto.request;

import lombok.Data;

@Data
public class ReviseJobRequest {
    private Long resultId;      // 수정 기반이 될 JobResult ID
    private String promptText;  // 새로운 프롬프트
}
