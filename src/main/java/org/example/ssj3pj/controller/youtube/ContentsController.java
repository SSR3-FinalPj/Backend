package org.example.ssj3pj.controller.youtube;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;
import org.example.ssj3pj.services.ContentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 콘텐츠 상세 정보 조회 API 컨트롤러
 */
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
@Slf4j
public class ContentsController {

    private final ContentsService contentsService;

    /**
     * 특정 영상의 상세 정보를 조회
     */
    @GetMapping("/{video_id}")
    public ResponseEntity<YoutubeContentDetailDto> getContentDetail(@PathVariable("video_id") String videoId) {
        try {
            log.info("콘텐츠 상세 정보 조회 요청: videoId={}", videoId);

            YoutubeContentDetailDto contentDetail = contentsService.getContentDetailByVideoId(videoId);

            log.info("콘텐츠 상세 정보 조회 성공: videoId={}", videoId);
            return ResponseEntity.ok(contentDetail);

        } catch (Exception e) {
            log.error("콘텐츠 조회 실패: videoId={}", videoId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
