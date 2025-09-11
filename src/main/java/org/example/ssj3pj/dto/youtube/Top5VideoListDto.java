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
public class Top5VideoListDto {
    private String channelId;
    private List<String> videos;
}
