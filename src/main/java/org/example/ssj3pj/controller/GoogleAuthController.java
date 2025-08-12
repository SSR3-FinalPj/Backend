package org.example.ssj3pj.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.GoogleOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/google")
public class GoogleAuthController {

    private final UsersRepository usersRepository;
    private final GoogleOAuthService googleOAuthService;
    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @GetMapping("/login")
    public void googleLoginRedirect(HttpServletResponse response) throws IOException {
        String Uri = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("https://www.googleapis.com/auth/youtube.readonly", StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent";

        response.sendRedirect(Uri);
    }


    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code,
                                      @AuthenticationPrincipal UserDetails userDetails) {

        // 1. 로그인된 유저 확인
        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

        // 2. 구글 토큰 요청 + 저장
        googleOAuthService.handleOAuthCallback(code, user);

        return ResponseEntity.ok("YouTube 계정 연동 완료");
    }
}
