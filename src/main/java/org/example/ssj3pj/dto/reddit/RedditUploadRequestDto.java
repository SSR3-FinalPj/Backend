package org.example.ssj3pj.dto.reddit;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RedditUploadRequestDto {
    @NotBlank
    private String subreddit; // 업로드할 서브레딧
    @NotBlank
    private String title;     // 포스트 제목
}
