package org.example.ssj3pj.controller.youtube;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.dashboard.DashboardRangeStats;
import org.example.ssj3pj.dto.youtube.ChannelInfoDto;
import org.example.ssj3pj.dto.youtube.UploadRangeDto;
import org.example.ssj3pj.dto.youtube.VideoListDto;
import org.example.ssj3pj.dto.youtube.VideoDetailDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.youtube.YouTubeChannelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * YouTube 채널 관련 API 컨트롤러
 * - 채널 메타 정보 조회
 * - 채널 영상 목록 조회
 */
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
public class    YouTubeChannelController {

    private final YouTubeChannelService youTubeChannelService;
    private final UsersRepository usersRepository;

    /**
     * 1. 채널 메타 정보 조회
     * GET /api/youtube/channelId
     * 
     * 현재 로그인된 사용자의 대표 YouTube 채널 ID 및 채널 메타 정보를 조회
     */
    @GetMapping("/channelId")
    public ResponseEntity<ChannelInfoDto> getChannelId(Principal principal) {
        try {
            String username = principal.getName();
            // 사용자 조회
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            // 채널 정보 조회
            ChannelInfoDto channelInfo = youTubeChannelService.getMyChannelInfo(user);
            
            log.info("채널 정보 조회 성공: userId={}, channelId={}", 
                    user.getId(), channelInfo.getChannelId());
            
            return ResponseEntity.ok(channelInfo);
        } catch (RuntimeException e) {
            
            // 구체적인 에러에 따라 HTTP 상태 코드 결정
            if (e.getMessage().contains("연동된 YouTube 채널이 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getMessage().contains("토큰")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 2. 채널 영상 목록 조회
     * GET /api/youtube/channel/{channelId}/videos
     * 
     * 특정 채널의 모든 영상 목록을 조회
     */
    @GetMapping("/channel/{channelId}/videos")
    public ResponseEntity<VideoListDto> getChannelVideos(
            @PathVariable String channelId,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "20") Integer maxResults,
            Principal principal) {
        try {
            log.info("채널 비디오 목록 조회 요청: channelId={}, pageToken={}, maxResults={}", 
                    channelId, pageToken, maxResults);
            // 입력 값 검증
            if (channelId == null || channelId.trim().isEmpty()) {
                log.warn("잘못된 채널 ID: {}", channelId);
                return ResponseEntity.badRequest().build();
            }
            
            if (maxResults <= 0 || maxResults > 50) {
                maxResults = 20; // 기본값으로 설정
            }

            String username = principal.getName();
            // 사용자 조회
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));


            // 비디오 목록 조회 (ES 기반)
            VideoListDto videoList = youTubeChannelService.getChannelVideos(user, channelId, pageToken, maxResults);
            
            log.info("채널 비디오 목록 조회 성공: channelId={}, 비디오 수={}", 
                    channelId, videoList.getVideos().size());
            
            return ResponseEntity.ok(videoList);
            
        } catch (RuntimeException e) {
            log.error("채널 비디오 목록 조회 실패: channelId={}", channelId, e);
            
            // 구체적인 에러에 따라 HTTP 상태 코드 결정
            if (e.getMessage().contains("존재하지 않는") || e.getMessage().contains("잘못된")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("채널 비디오 목록 조회 중 예상치 못한 오류: channelId={}", channelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /**
     * UserDetails에서 Users 엔티티 추출
     */
    private Users getUserFromDetails(UserDetails userDetails) {
        return usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userDetails.getUsername()));
    }
    // ② 기간(일별 배열)
    @GetMapping("/uploadRange")
    public ResponseEntity<UploadRangeDto> uploadRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String channelId,
            Principal principal
    ) {
        try {
            String username = principal.getName();
            // 사용자 조회
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            LocalDate s = LocalDate.parse(startDate);
            LocalDate e = LocalDate.parse(endDate);
            return ResponseEntity.ok(youTubeChannelService.uploadRange(user, s, e, channelId));
        } catch (RuntimeException e) {
            log.error("채널 비디오 목록 조회 실패: channelId={}", channelId, e);

            // 구체적인 에러에 따라 HTTP 상태 코드 결정
            if (e.getMessage().contains("존재하지 않는") || e.getMessage().contains("잘못된")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("채널 비디오 목록 조회 중 예상치 못한 오류: channelId={}", channelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
