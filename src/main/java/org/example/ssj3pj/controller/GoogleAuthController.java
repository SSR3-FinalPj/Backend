package org.example.ssj3pj.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.GoogleOAuthService;
import org.example.ssj3pj.services.OAuthStateService;
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
    private final OAuthStateService oAuthStateService;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.redirect-uri}")
    private String redirectUri;

 // google url 전송
 @Tag(name = "googleLogin", description = "구글 로그인")
 @GetMapping("/login-url")
 public ResponseEntity<String> getGoogleLoginUrl(@AuthenticationPrincipal UserDetails userDetails) {
     Users user = usersRepository.findByUsername(userDetails.getUsername())
             .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

     String state = oAuthStateService.issueState(user.getId());

     //권한 설정
     String scopes = String.join(" ",
             "https://www.googleapis.com/auth/youtube.readonly",
             "https://www.googleapis.com/auth/yt-analytics.readonly",
             "https://www.googleapis.com/auth/youtube.upload"

     );

     String url = "https://accounts.google.com/o/oauth2/v2/auth"
             + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
             + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
             + "&response_type=code"
             + "&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8)
             + "&access_type=offline"
             + "&include_granted_scopes=true"
             + "&prompt=consent"
             + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

     return ResponseEntity.ok(url);
 }


//
//    @GetMapping("/login")
//    public void googleLoginRedirect(HttpServletResponse response,
//                                    @AuthenticationPrincipal UserDetails userDetails) throws IOException {
//        Users user = usersRepository.findByUsername(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));
//
//        String state = oAuthStateService.issueState(user.getId());
//
//        String uri = "https://accounts.google.com/o/oauth2/v2/auth"
//                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
//                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
//                + "&response_type=code"
//                + "&scope=" + URLEncoder.encode("https://www.googleapis.com/auth/youtube.readonly", StandardCharsets.UTF_8)
//                + "&access_type=offline"
//                + "&prompt=consent"
//                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
//
//        response.sendRedirect(uri);
//    }

    @Tag(name = "googleLogin", description = "구글페이지 콜백")
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {
        System.out.println("[callback] arrived, state=" + state + " code=" + (code != null ? code.substring(0,8)+"..." : "null"));

        if (state == null || state.isBlank()) {
            response.getWriter().write("state 누락");
            return;
        }

        Long userId = oAuthStateService.consumeState(state);
        if (userId == null) {
            response.getWriter().write("유효하지 않은 state(만료/위조/재사용)");
            return;
        }

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

        googleOAuthService.handleOAuthCallback(code, user);

        // ✅ HTML + JS로 응답해서 부모창에 알리고, 팝업 닫기
        String html = """
        """;

        response.setStatus(200);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
    }
}
