// src/main/java/org/example/ssj3pj/sse/SseController.java
package org.example.ssj3pj.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.config.SseConfig.SseSettings;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.SseHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify")
public class SseController {

    private final SseSettings conf;
    private final JwtUtils jwtUtils;
    private final SseHub sseHub;

    @CrossOrigin(origins = {"http://localhost:5173","http://localhost:3000"}) // 필요시 조정/제거
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String userId,
                             @RequestParam(name = "#{@sseConfig.sseSettings.tokenQueryParam()}", required = false) String sse_token // 주입 안 되므로 아래에서 수동 처리
    ) {
        // 1) 운영: sse_token(쿼리)에 담긴 JWT로 인증
        String tokenParamName = conf.tokenQueryParam(); // ex: sse_token
        // 스프링 EL로 파라미터 바인딩하기 까다로워서 수동 추출:
        if (sse_token == null) {
            // 스프링이 위의 EL을 못 채우는 경우가 있으므로, 아래처럼 한 번 더 안전망 걸기
            // 단, 여기서는 단순화를 위해 @RequestParam으로 못 받은 경우를 BAD_REQUEST로 처리
        }

        // RequestParam 맵 전체에서 직접 꺼내고 싶으면 HttpServletRequest로 받아서 req.getParameter(tokenParamName) 사용해도 됨.
        String token = sse_token; // 위에서 파라미터로 받았다고 가정

        if (token != null && !token.isBlank()) {
            try {
                jwtUtils.validateJwtToken(token); // 유효성 검사(만료/서명)
                String sub = jwtUtils.getUserName(token); // subject = userId
                Long uid = Long.valueOf(sub);
                return sseHub.subscribe(uid);
            } catch (Exception e) {
                log.warn("SSE JWT invalid: {}", e.getMessage());
                throw new ResponseStatusException(UNAUTHORIZED, "Invalid token");
            }
        }

        // 2) (옵션) 로컬 개발 우회: ?userId=1 (운영에서 conf.devAllowUserIdParam=false)
        if (conf.devAllowUserIdParam()) {
            if (userId == null || userId.isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "userId is required in dev mode");
            }
            try {
                return sseHub.subscribe(Long.valueOf(userId));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(BAD_REQUEST, "userId must be a number");
            }
        }

        throw new ResponseStatusException(UNAUTHORIZED, "Missing token");
    }

    /** 로컬 테스트용 (운영에서 제거 권장) */
    @PostMapping("/_dev/trigger")
    public void trigger(@RequestParam Long userId) {
        sseHub.notifyVideoReady(userId);
    }
}
