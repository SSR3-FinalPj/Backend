package org.example.ssj3pj.dto.reddit;

import lombok.Builder;
import lombok.Data;
import java.util.List;


// 게시글 요약
@Data
@Builder
public class RedditSummaryDto {
    private String id;                 // 게시글 id
    private String title;              // 제목
    private String selftext;           // 본문
    private String subreddit;          // 카테고리
    private String url;                // 원본 URL
    private String createdAt;          // 작성 시각 (ISO-8601)
    private Integer score;             // 점수
    private Double upvoteRatio;        // 좋아요 비율
    private Integer upvotesEstimated;  // 추정 업보트
    private Integer downvotesEstimated;// 추정 다운보트
    private Integer numComments;       // 댓글 수
    private Boolean over18;            // NSFW 여부
    private Integer commentCount;      // commentcount
    private List<RedditCommentDto> comments; // 댓글 리스트
}
