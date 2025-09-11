package org.example.ssj3pj.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비디오 아이템 DTO (비디오 목록에서 사용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoItemDto {
    private String videoId;

    private String title;

    private String thumbnail;

    private String publishedAt;

    private String url;

    private VideoStatisticsDto statistics;
}
