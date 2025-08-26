package org.example.ssj3pj.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채널 메타 정보 조회 API 응답 DTO
 * GET /api/youtube/channelId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelInfoDto {
    /**
     * YouTube 채널 ID
     */
    private String channelId;
    
    /**
     * 채널 제목/이름
     */
    private String channelTitle;
    
    /**
     * 커스텀 URL (필요시 사용)
     * 예: https://youtube.com/@mychannel
     */
    // private String customUrl;
}
