package org.example.ssj3pj.dto.reddit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;   // ✅ int → Integer 로 수정 (wrapper)

    public static TokenResponseDto from(Map<String, Object> m) {
        if (m == null || m.get("access_token") == null) {
            throw new IllegalStateException("Reddit token parse failed: " + m);
        }
        // 기본값 3600 보장
        Number exp = (Number) m.getOrDefault("expires_in", 3600);
        return new TokenResponseDto(
                (String) m.get("access_token"),
                (String) m.get("refresh_token"),
                (String) m.get("token_type"),
                exp != null ? exp.intValue() : 3600   // ✅ Integer로 안전 변환
        );
    }
}
