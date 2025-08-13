package org.example.ssj3pj.dto.response;

public record GoogleTokenRefreshResponse(
    String access_token,
    Integer expires_in,
    String token_type,
    String scope,
    String refresh_token // 보통 null, 있으먄 회전된 것
) {}
