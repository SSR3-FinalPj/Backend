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

    // ✅ 기존 yml의 값이 "밀리초(ms)"로 보이므로, 아래는 "그대로 ms로 사용"
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;   // 예: 900000 (15분)
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;  // 예: 1209600000 (14일)

    @Value("${jwt.secret}")
    private String jwtSecret;

    // (선택) 발급자/오디언스 설정
    @Value("${jwt.issuer:ssj3pj}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** ===== Access / Refresh 생성 ===== */

    /** Access 토큰 생성: userId, role, jti 포함 */
    public String createAccess(String userId, String role, String jti) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpirationMs);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId)
                .setAudience("browser")
                .claim("role", role)
                .setId(jti)              // ✅ jti
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Refresh 토큰 생성: userId, deviceId, jti 포함 */
    public String createRefresh(String userId, String deviceId, String jti) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenExpirationMs);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId)
                .setAudience("refresh")
                .claim("device", deviceId) // ✅ 디바이스 구분
                .setId(jti)                // ✅ jti
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 공용 파서: 서명/만료까지 검증된 Jws<Claims> 반환 */
    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    /** ===== 기존 메서드와의 호환(원하면 계속 사용 가능) ===== */

    /** (구) username만으로 Access를 만들던 메서드 → 내부적으로 createAccess 사용하도록 변경 */
    @Deprecated
    public String generateTokenFromUsername(String username) {
        // role/jti가 필요 없는 구호환. 실제 운영에선 createAccess(...) 사용 권장
        return createAccess(username, "USER", java.util.UUID.randomUUID().toString());
    }

    /** (구) username만 받던 Refresh → deviceId/jti 없는 구호환. 새 코드에선 createRefresh 사용 */
    @Deprecated
    public String generateRefreshToken(String username) {
        return createRefresh(username, "unknown-device",
                java.util.UUID.randomUUID().toString());
    }

    /** subject(userId)만 꺼내는 편의 함수 (필요 시 사용) */
    public String getUserName(String token) {
        return parse(token).getBody().getSubject();
    }

    /** 기존 validate 스타일도 유지 (필요 없으면 제거 가능) */
    public boolean validateJwtToken(String token) {
        try {
            parse(token); // 서명/만료 검증
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            throw new TokenValidationException("유효하지 않은 JWT 서명");
        } catch (ExpiredJwtException e) {
            throw new TokenValidationException("JWT 토큰 만료됨");
        } catch (UnsupportedJwtException e) {
            throw new TokenValidationException("지원하지 않는 JWT");
        } catch (IllegalArgumentException e) {
            throw new TokenValidationException("JWT 클레임이 비어 있음");
        }
    }
}
