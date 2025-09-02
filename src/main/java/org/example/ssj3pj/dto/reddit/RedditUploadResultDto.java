package org.example.ssj3pj.dto.reddit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedditUploadResultDto {
    private boolean success;     // 업로드 성공 여부
    private String errorMessage; // 실패 시 에러 메시지
    private Long resultId;       // JobResult ID
    private String title;        // 업로드된 글 제목
    private String postId;       // Reddit 포스트 ID
    private String postUrl;      // Reddit 포스트 URL (https://www.reddit.com/comments/{postId})
}
