package org.example.ssj3pj.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//kafka로 토큰 보낼 dto
@Data
@NoArgsConstructor   // 기본 생성자
@AllArgsConstructor  // 모든 필드를 받는 생성자
public class GoogleAccessTokenEvent {
    private Long userId;
    private String provider;
    private String accessToken;
    private long expiresAtEpochSec;
    private String event;
    private String youtubeChannelId;
}
