// src/main/java/org/example/ssj3pj/security/jwt/JwtUtils.java
package org.example.ssj3pj.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.exception.TokenValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer:ssj3pj}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** ===== 발급 (함수 이름 그대로, uid 필수) ===== */

    /** Access: sub=username(또는 고정 주체) + uid(Long) 클레임 포함 */
    public String createAccess(String subject, String role, String jti, Long uid) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpirationMs);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)          // 보통 username (Redis/로그와 호환)
                .setAudience("browser")
                .claim("role", role)
                .claim("uid", uid)            // ✅ numeric user id
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Refresh: sub=username + device + uid(Long) */
    public String createRefresh(String subject, String deviceId, String jti, Long uid) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenExpirationMs);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)          // username
                .setAudience("refresh")
                .claim("device", deviceId)
                .claim("uid", uid)            // ✅ numeric user id
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** ===== 파싱/검증 ===== */
    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public void validateJwtToken(String token) {
        try {
            parse(token);
        } catch (ExpiredJwtException e) {
            throw new TokenValidationException("JWT 토큰 만료");
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenValidationException("유효하지 않은 JWT");
        }
    }

    /** uid(Long) 필수 추출 (없으면 예외) */
    public Long getUidAsLong(String token) {
        Object uid = parse(token).getBody().get("uid");
        if (uid == null) throw new TokenValidationException("JWT에 uid 클레임이 없습니다");
        return Long.valueOf(String.valueOf(uid));
    }

    /** 필요 시 주체(subject=username) 접근 */
    public String getUserName(String token) {
        return parse(token).getBody().getSubject();
    }
}
