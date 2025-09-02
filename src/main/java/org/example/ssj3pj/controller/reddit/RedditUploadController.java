package org.example.ssj3pj.controller.reddit;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.reddit.RedditUploadRequestDto;
import org.example.ssj3pj.dto.reddit.RedditUploadResultDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.Reddit.RedditJobUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reddit")
@RequiredArgsConstructor
@Slf4j
public class RedditUploadController {

    private final RedditJobUploadService redditJobUploadService;
    private final UsersRepository usersRepository;
    private final JwtUtils jwtUtils;

    /**
     * JobResult를 Reddit에 업로드
     */
    @PostMapping("/upload/{resultId}")
    public ResponseEntity<RedditUploadResultDto> uploadJobResult(
            @PathVariable Long resultId,
            @Valid @RequestBody RedditUploadRequestDto request,
            HttpServletRequest httpRequest) {

        try {
            Long userId = getUserIdFromRequest(httpRequest);

            log.info("Reddit 업로드 요청: resultId={}, userId={}, subreddit={}, title={}",
                    resultId, userId, request.getSubreddit(), request.getTitle());

            RedditUploadResultDto result = redditJobUploadService.uploadJobResult(
                    resultId, request, userId);

            if (result.isSuccess()) {
                log.info("Reddit 업로드 성공: postId={}", result.getPostId());
                return ResponseEntity.ok(result);
            } else {
                log.warn("Reddit 업로드 실패: {}", result.getErrorMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Reddit 업로드 처리 중 오류: resultId={}", resultId, e);

            RedditUploadResultDto errorResult = RedditUploadResultDto.builder()
                    .success(false)
                    .errorMessage("업로드 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .resultId(resultId)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * 요청에서 사용자 ID 추출
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer 토큰이 필요합니다");
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtUtils.getUserName(token);

            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다"));

            return user.getId();

        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다");
        }
    }
}
