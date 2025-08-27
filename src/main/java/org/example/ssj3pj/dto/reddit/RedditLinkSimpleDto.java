package org.example.ssj3pj.dto.reddit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor
public class RedditLinkSimpleDto {
    private boolean linked;
    private String  username;     // reddit_username
    private Instant expiresAt;    // access_token 만료(참고용)
}