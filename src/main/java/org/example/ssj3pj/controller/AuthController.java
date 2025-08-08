package org.example.ssj3pj.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ssj3pj.dto.request.LoginRequest;
import org.example.ssj3pj.dto.request.RefreshRequest;
import org.example.ssj3pj.dto.response.TokenResponse;
import org.example.ssj3pj.entity.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    /**
     * 로그인 → Access + Refresh 토큰 발급
     */
    @Tag(name = "Auth", description = "로그인/토큰 관리 API")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        Users user = usersRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtils.generateTokenFromUsername(user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUsername());

        refreshTokenService.saveRefreshToken(
                user.getUsername(),
                refreshToken,
                jwtUtils.getRefreshTokenExpiration()
        );

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    /**
     * RefreshToken 기반 AccessToken 재발급
     */
    @Tag(name = "Auth", description = "리프레쉬 토큰 관련")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtUtils.validateJwtToken(refreshToken)) {
            return ResponseEntity.status(401).body("유효하지 않은 RefreshToken입니다.");
        }

        String username = jwtUtils.getUserNameFromJwtToken(refreshToken);

        if (!refreshTokenService.isValidRefreshToken(username, refreshToken)) {
            return ResponseEntity.status(401).body("저장된 RefreshToken과 일치하지 않습니다.");
        }

        String newAccessToken = jwtUtils.generateTokenFromUsername(username);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, refreshToken));
    }
}
