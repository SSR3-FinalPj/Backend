package org.example.ssj3pj.dto.youtube;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YoutubeCommentDto {
    private String commentId;             // 댓글 ID
    private String videoId;               // 비디오 ID
    private String author;                // 댓글 작성자 이름
    private String comment;               // 댓글 내용
    private Integer likeCount;            // 좋아요 수 1
    private String publishedAt;           // 댓글 작성 시간 1
    private String updatedAt;             // 댓글 수정 시간 x
    private Integer totalReplyCount;      // 답글 수
}
