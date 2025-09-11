package org.example.ssj3pj.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;   // ★ 추가
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request, HttpServletResponse response) { // ★ response 추가
        // ▶ 프록시 버퍼링/캐시 방지(권장)
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        final String paramName = conf.tokenQueryParam(); // 예: "sse_token"
        final String token = request.getParameter(paramName);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing query param: " + paramName);
        }

        try {
            jwtUtils.validateJwtToken(token);
            Long uid = jwtUtils.getUidAsLong(token);      // uid 클레임 필수
            return sseHub.subscribe(uid);
        } catch (Exception e) {
            log.warn("SSE token invalid: {}", e.getMessage());
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
