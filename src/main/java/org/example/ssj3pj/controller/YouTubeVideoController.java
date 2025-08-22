package org.example.ssj3pj.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.VideoDetailDto;
import org.example.ssj3pj.services.YouTubeVideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * YouTube 비디오 관련 API 컨트롤러
 * - 단일 영상 상세 정보 조회
 */
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
public class YouTubeVideoController {

    private final YouTubeVideoService youTubeVideoService;

    /**
     * 3. 단일 영상 상세 조회
     * GET /api/youtube/video/{videoId}
     * 
     * 특정 영상의 상세 정보를 조회 (title, thumbnail, publishedAt, url, statistics)
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<VideoDetailDto> getVideoDetail(@PathVariable String videoId) {
        try {
            log.info("비디오 상세 정보 조회 요청: videoId={}", videoId);
            
            // 입력 값 검증
            if (videoId == null || videoId.trim().isEmpty()) {
                log.warn("잘못된 비디오 ID: {}", videoId);
                return ResponseEntity.badRequest().build();
            }
            
            // 비디오 상세 정보 조회
            VideoDetailDto videoDetail = youTubeVideoService.getVideoDetail(videoId);
            
            log.info("비디오 상세 정보 조회 성공: videoId={}, title={}", 
                    videoId, videoDetail.getTitle());
            
            return ResponseEntity.ok(videoDetail);
            
        } catch (RuntimeException e) {
            log.error("비디오 상세 정보 조회 실패: videoId={}", videoId, e);
            
            // 구체적인 에러에 따라 HTTP 상태 코드 결정
            if (e.getMessage().contains("존재하지 않는 비디오 ID")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getMessage().contains("API 호출 실패")) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("비디오 상세 정보 조회 중 예상치 못한 오류: videoId={}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
