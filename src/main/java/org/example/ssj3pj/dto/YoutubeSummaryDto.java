package org.example.ssj3pj.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeSummaryDto {
    
    private String videoId;               // 영상_id
    private String title;                 // 제목
    private String description;           // 설명
    private String channelTitle;          // 채널_제목
    private String channelId;             // 채널_id
    private String uploadDate;            // 업로드_날짜
    private Integer viewCount;            // 조회수
    private Integer likeCount;            // 좋아요 수
    private Integer commentCount;         // 댓글 수
    private String categoryId;            // 카테고리_id
    private String[] tags;                // 태그
    private YoutubeThumbnailDto thumbnailUrl;    // 썸네일_url
    private String videoPlayer;           // 영상_플레이어
    private List<YoutubeCommentDto> comments;  // 댓글
}
