package org.example.ssj3pj.controller.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.reddit.RedditContentDetailDto;
import org.example.ssj3pj.dto.request.CommentAnalyticRequest;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.ContentsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * 콘텐츠 상세 정보 조회 및 분석 API 컨트롤러
 */
@RestController
@RequestMapping("/api/reddit") // Base path changed to /api/youtube
@RequiredArgsConstructor
@Slf4j
public class RedditContentsController {

    private final ContentsService contentsService;
    private final JwtUtils jwtUtils;

    @GetMapping("/contents/{post_id}")
    public ResponseEntity<RedditContentDetailDto> getContentDetail(@PathVariable("post_id") String postId, HttpServletRequest request) {
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

            log.info("콘텐츠 상세 정보 조회 요청: postId={}", postId);

            RedditContentDetailDto contentDetail = contentsService.getContentDetailByPostId(postId, userName);

            log.info("콘텐츠 상세 정보 조회 성공: postId={}", postId);
            return ResponseEntity.ok(contentDetail);

        } catch (Exception e) {
            log.error("콘텐츠 조회 실패: postId={}", postId, e);
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
            JsonNode aiResponse = contentsService.analyzeRDComments(request.getVideoId(), username);
            return ResponseEntity.ok(aiResponse); // FE에 그대로 전달
        } catch (Exception e) {
            log.error("댓글 분석 요청 실패: videoId={}", request.getVideoId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}