package org.example.ssj3pj.controller.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.ContentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 콘텐츠 상세 정보 조회 API 컨트롤러
 */
import org.example.ssj3pj.dto.request.CommentAnalyticRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * 콘텐츠 상세 정보 조회 및 분석 API 컨트롤러
 */
@RestController
@RequestMapping("/api/youtube") // Base path changed to /api/youtube
@RequiredArgsConstructor
@Slf4j
public class YoutubeContentsController {

    private final ContentsService contentsService;
    private final JwtUtils jwtUtils;

    /**
     * 특정 영상의 상세 정보를 조회
     */
    @GetMapping("/contents/{video_id}")
    public ResponseEntity<YoutubeContentDetailDto> getContentDetail(@PathVariable("video_id") String videoId, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
            }
            String token = auth.substring(7);

            String userName;
            try {
                userName = jwtUtils.getUserName(token);
            } catch (RuntimeException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
            }

            log.info("콘텐츠 상세 정보 조회 요청: videoId={}", videoId);

            YoutubeContentDetailDto contentDetail = contentsService.getContentDetailByVideoId(videoId, userName);

            log.info("콘텐츠 상세 정보 조회 성공: videoId={}", videoId);
            return ResponseEntity.ok(contentDetail);

        } catch (Exception e) {
            log.error("콘텐츠 조회 실패: videoId={}", videoId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 유튜브 댓글 분석을 요청
     */
    @PostMapping("/commentAnalystic")
    public ResponseEntity<JsonNode> analyzeComments(@RequestBody CommentAnalyticRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = principal.getName();

        try {
            JsonNode aiResponse = contentsService.analyzeComments(request.getVideoId(), username);
            return ResponseEntity.ok(aiResponse); // FE에 그대로 전달
        } catch (Exception e) {
            log.error("댓글 분석 요청 실패: videoId={}", request.getVideoId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}