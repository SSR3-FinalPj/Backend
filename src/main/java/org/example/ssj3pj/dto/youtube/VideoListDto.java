package org.example.ssj3pj.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 채널 영상 목록 조회 API 응답 DTO
 * GET /api/youtube/channel/{channelId}/videos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoListDto {
    /**
     * 채널 ID
     */
    private String channelId;
    
    /**
     * 비디오 목록
     */
    private List<VideoItemDto> videos;
    
    /**
     * 다음 페이지 토큰 (YouTube API 페이지네이션용)
     */
    private String nextPageToken;
    
    /**
     * 페이지 정보
     */
    private PageInfoDto pageInfo;
}
