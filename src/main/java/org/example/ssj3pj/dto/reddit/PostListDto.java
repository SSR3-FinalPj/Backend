package org.example.ssj3pj.dto.reddit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ssj3pj.dto.youtube.PageInfoDto;
import org.example.ssj3pj.dto.youtube.VideoItemDto;

import java.util.List;

/**
 * 채널 영상 목록 조회 API 응답 DTO
 * GET /api/youtube/channel/{channelId}/videos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostListDto {
    /**
     * 채널 ID
     */
    private String channelId;
    
    /**
     * 비디오 목록
     */
    private List<RedditContentDetailDto> posts;

}
