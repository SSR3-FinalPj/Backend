package org.example.ssj3pj.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadVideoDetailDto {
    private String title;                 // 제목
    private String description;           // 설명
    private String channelTitle;          // 채널_제목 x
    private String uploadDate;            // 업로드_날짜
    private Integer viewCount;            // 조회수
    private Integer likeCount;            // 좋아요 수
    private Integer commentCount;         // 댓글 수
}
