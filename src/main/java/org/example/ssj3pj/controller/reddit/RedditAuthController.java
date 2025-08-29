package org.example.ssj3pj.controller.reddit;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.Reddit.RedditLinkStatusService;
import org.example.ssj3pj.services.Reddit.RedditOAuthService;
import org.example.ssj3pj.services.OAuthStateService;
import org.example.ssj3pj.dto.reddit.RedditLinkSimpleDto;
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
@RequestMapping("/api/reddit")
public class RedditAuthController {

    private static final String PROVIDER = "reddit";

    private final UsersRepository usersRepository;
    private final OAuthStateService oAuthStateService;
    private final RedditOAuthService redditOAuthService;
    private final RedditLinkStatusService statusService;

    @Value("${reddit.client-id}")     private String clientId;
    @Value("${reddit.redirect-uri}")  private String redirectUri;
    @Value("${reddit.authorize-url}") private String authorizeUrl;
    @Value("${reddit.scopes}")        private String scopes; // 예: "identity read submit"

    @Tag(name = "redditLogin", description = "Reddit 로그인 URL 생성")
    @GetMapping("/login-url")
    public ResponseEntity<String> getRedditLoginUrl(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthenticated");
        }

        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));

        String state = oAuthStateService.issueState(PROVIDER, user.getId());

        // Reddit은 permanent로 요청해야 refresh_token 발급됨
        String url = authorizeUrl
                + "?client_id="     + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&response_type=" + "code"
                + "&state="         + URLEncoder.encode(state, StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&duration="      + "permanent"
                + "&scope="         + URLEncoder.encode(scopes, StandardCharsets.UTF_8);

        return ResponseEntity.ok(url);
    }

    @Tag(name = "redditLogin", description = "Reddit 로그인")
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {
        if (state == null || state.isBlank()) {
            response.setStatus(400);
            response.getWriter().write("state 누락");
            return;
        }

        Long userId = oAuthStateService.consumeState("reddit", state);
        if (userId == null) {
            response.setStatus(400);
            response.getWriter().write("유효하지 않은 state(만료/위조/재사용)");
            return;
        }

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));

        redditOAuthService.handleOAuthCallback(code, user);

        String appRedirect = "http://localhost:5173"; //수정 예정

        String html = """
    <!doctype html>
    <html><head><meta charset="utf-8"><title>Reddit Linked</title></head>
    <body>
    <script>
      (function () {
        try {
          if (window.opener && !window.opener.closed) {
            try { window.opener.postMessage({ type: "REDDIT_LINKED", ok: true }, "*"); } catch (e) {}
            window.close();
            setTimeout(function(){ try{ window.open('','_self'); window.close(); }catch(e){} }, 0);
            return;
          }
        } catch (e) {}
        // 현재 탭으로 열린 경우: 프론트로 이동
        location.replace("%s");
      })();
    </script>
    연결되었습니다. 이 창은 자동으로 닫히거나 리다이렉트됩니다.
    </body></html>
    """.formatted(appRedirect);

        response.setStatus(200);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
    }


    @Tag(name = "redditLogin", description = "Reddit 로그인")
    @GetMapping("/status")
    public ResponseEntity<RedditLinkSimpleDto> status(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));
        return ResponseEntity.ok(statusService.getStatus(user.getId()));
    }
}
