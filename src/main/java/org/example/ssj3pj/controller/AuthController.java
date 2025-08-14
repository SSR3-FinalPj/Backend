package org.example.ssj3pj.controller;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.request.LoginRequest;
// import org.example.ssj3pj.dto.request.RefreshRequest; // 더 이상 필요 없음 (쿠키 사용)
import org.example.ssj3pj.dto.response.TokenResponse;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.redis.RefreshStoreRedis; // ★ Step4에서 만든 Redis 저장소
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshStoreRedis refreshStore; // ★ Redis에 jti/만료/디바이스 저장

    /**
     * 로그인 → Access(JSON) + Refresh(HttpOnly 쿠키) 발급
     */
    @Tag(name = "Auth", description = "로그인/토큰 관리 API")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request,
                                               @RequestHeader(value = "User-Agent", required = false) String ua,
                                               HttpServletResponse res) {
        Users user = usersRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 디바이스 식별 (간단 버전: UA 해시)
        String deviceId = Integer.toHexString((ua == null ? "unknown" : ua).hashCode());

        // jti 생성
        String rJti = UUID.randomUUID().toString();
        String aJti = UUID.randomUUID().toString();

        // Refresh(쿠키용) + Access(JSON용) 생성
        // role 없으면 "USER" 등 기본값 사용
        String refreshToken = jwtUtils.createRefresh(user.getUsername(), deviceId, rJti);
        String accessToken  = jwtUtils.createAccess(user.getUsername(), "USER", aJti);

        // Redis에 현재 유효한 refresh jti 저장(+TTL)
        Instant refreshExp = jwtUtils.parse(refreshToken).getBody().getExpiration().toInstant();
        refreshStore.save(user.getUsername(), deviceId, rJti, refreshExp);

        // HttpOnly Refresh 쿠키 세팅 (개발 환경에서 HTTPS가 아니면 secure(false)로 임시 테스트)
        ResponseCookie cookie = ResponseCookie.from("rt", refreshToken)
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/api/auth") // 최소 범위
                .maxAge(Duration.between(Instant.now(), refreshExp))
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 응답 바디엔 Access만 내려주세요(기존 DTO 호환 위해 refresh=null로 반환)
        return ResponseEntity.ok(new TokenResponse(accessToken, null));
    }

    /**
     * Refresh 쿠키 기반 AccessToken 재발급(회전+재사용 감지)
     */
    @Tag(name = "Auth", description = "리프레시 토큰 관련")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(name = "rt", required = false) String refreshCookie,
                                                HttpServletResponse res) {
        if (refreshCookie == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh cookie not found"));
        }

        try {
            Claims body = jwtUtils.parse(refreshCookie).getBody();
            String username = body.getSubject();
            String deviceId = (String) body.get("device");
            String jti      = body.getId();

            // 서버 저장된 현재 유효 jti 조회
            String currentJti = refreshStore.getJti(username, deviceId);
            if (currentJti == null) {
                expireCookie(res);
                return ResponseEntity.status(401).body(Map.of("error", "No active refresh session"));
            }

            // 재사용 감지: 쿠키 jti != Redis jti → 탈취 의심 → 전체 강제 로그아웃
            if (!jti.equals(currentJti)) {
                refreshStore.revokeAll(username);
                expireCookie(res);
                return ResponseEntity.status(401).body(Map.of("error", "Refresh token reuse detected"));
            }

            // 회전(rotate): 새 refresh/새 access 발급
            String newRJti     = UUID.randomUUID().toString();
            String newAJti     = UUID.randomUUID().toString();
            String newRefresh  = jwtUtils.createRefresh(username, deviceId, newRJti);
            String newAccess   = jwtUtils.createAccess(username, "USER", newAJti);

            Instant exp = jwtUtils.parse(newRefresh).getBody().getExpiration().toInstant();
            refreshStore.save(username, deviceId, newRJti, exp);

            // 새 쿠키로 교체
            res.addHeader(HttpHeaders.SET_COOKIE,
                    ResponseCookie.from("rt", newRefresh)
                            .httpOnly(true).secure(true).sameSite("Strict")
                            .path("/api/auth").maxAge(Duration.between(Instant.now(), exp)).build()
                            .toString()
            );
// secure false-> true로 바꾸기
            // Access만 반환
            return ResponseEntity.ok(new TokenResponse(newAccess, null));
        } catch (Exception e) {
            expireCookie(res);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }
    }

    /**
     * 로그아웃: 현재 디바이스 refresh 무효화 + 쿠키 제거
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "rt", required = false) String refreshCookie,
                                    HttpServletResponse res) {
        if (refreshCookie != null) {
            Claims body = jwtUtils.parse(refreshCookie).getBody();
            String username = body.getSubject();
            String deviceId = (String) body.get("device");
            refreshStore.delete(username, deviceId);
        }
        expireCookie(res);
        return ResponseEntity.ok().build();
    }
// secure false-> true로 바꾸기
    private void expireCookie(HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("rt", "")
                        .httpOnly(true).secure(false).sameSite("Strict")
                        .path("/api/auth").maxAge(0).build()
                        .toString()
        );
    }
}
