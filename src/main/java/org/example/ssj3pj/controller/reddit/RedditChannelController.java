package org.example.ssj3pj.controller.reddit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.reddit.PostListDto;
import org.example.ssj3pj.dto.reddit.RDUploadRangeDto;
import org.example.ssj3pj.dto.youtube.ChannelInfoDto;
import org.example.ssj3pj.dto.youtube.YTUploadRangeDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.Reddit.RedditChannelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

/**
 * YouTube 채널 관련 API 컨트롤러
 * - 채널 메타 정보 조회
 * - 채널 영상 목록 조회
 */
@RestController
@RequestMapping("/api/reddit")
@RequiredArgsConstructor
@Slf4j
public class RedditChannelController {

    private final RedditChannelService redditChannelService;
    private final UsersRepository usersRepository;

    @GetMapping("/channelId")
    public ResponseEntity<ChannelInfoDto> getChannelId(Principal principal) {
        try {
            String username = principal.getName();
            // 사용자 조회
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            // 채널 정보 조회
            ChannelInfoDto channelInfo = redditChannelService.getMyChannelInfo(user);
            
            log.info("채널 정보 조회 성공: userId={}, channelId={}", 
                    user.getId(), channelInfo.getChannelId());
            
            return ResponseEntity.ok(channelInfo);
        } catch (RuntimeException e) {
            
            // 구체적인 에러에 따라 HTTP 상태 코드 결정
            if (e.getMessage().contains("연동된 Reddit 계정이 없습니다")) {
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

    @GetMapping("/channel/{channelName}/posts")
    public ResponseEntity<PostListDto> getChannelPosts(
            @PathVariable String channelName,
            Principal principal) {
        try {
            log.info("채널 비디오 목록 조회 요청: channelName={}",
                    channelName);


            String username = principal.getName();
            // 사용자 조회
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));


            // 비디오 목록 조회 (ES 기반)
            PostListDto postList = redditChannelService.getChannelPosts(user, channelName);
            
            log.info("채널 비디오 목록 조회 성공: channelName={}, 비디오 수={}",
                    channelName, postList.getPosts().size());
            
            return ResponseEntity.ok(postList);
            
        } catch (RuntimeException e) {
            log.error("채널 비디오 목록 조회 실패: channelName={}", channelName, e);
            
            // 구체적인 에러에 따라 HTTP 상태 코드 결정
            if (e.getMessage().contains("존재하지 않는") || e.getMessage().contains("잘못된")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("채널 비디오 목록 조회 중 예상치 못한 오류: channelName={}", channelName, e);
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

    @GetMapping("/uploadRange")
    public ResponseEntity<RDUploadRangeDto> uploadRange(
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
            return ResponseEntity.ok(redditChannelService.uploadRange(user, s, e, channelId));
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
