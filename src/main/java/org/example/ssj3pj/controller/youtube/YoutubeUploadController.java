package org.example.ssj3pj.controller.youtube;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.request.YoutubeUploadRequestDto;
import org.example.ssj3pj.dto.response.YoutubeUploadResultDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.youtube.YoutubeJobUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * YouTube 업로드 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
public class YoutubeUploadController {

    private final YoutubeJobUploadService youtubeJobUploadService;
    private final UsersRepository usersRepository;
    private final JwtUtils jwtUtils;

    /**
     * JobResult를 YouTube에 업로드
     */
    @PostMapping("/upload/{jobId}/result/{resultId}")
    public ResponseEntity<YoutubeUploadResultDto> uploadJobResult(
            @PathVariable Long jobId,
            @PathVariable Long resultId,
            @Valid @RequestBody YoutubeUploadRequestDto request,
            HttpServletRequest httpRequest) {
        
        try {
            // 사용자 인증 및 조회
            Long userId = getUserIdFromRequest(httpRequest);
            
            log.info("YouTube 업로드 요청: jobId={}, resultId={}, userId={}, title={}", 
                    jobId, resultId, userId, request.getTitle());
            
            // 업로드 실행
            YoutubeUploadResultDto result = youtubeJobUploadService.uploadJobResult(
                    jobId, resultId, request, userId);
            
            if (result.isSuccess()) {
                log.info("YouTube 업로드 성공: videoId={}", result.getVideoId());
                return ResponseEntity.ok(result);
            } else {
                log.warn("YouTube 업로드 실패: {}", result.getErrorMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("YouTube 업로드 처리 중 오류: jobId={}, resultId={}", jobId, resultId, e);
            
            YoutubeUploadResultDto errorResult = YoutubeUploadResultDto.builder()
                    .success(false)
                    .errorMessage("업로드 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .jobId(jobId)
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