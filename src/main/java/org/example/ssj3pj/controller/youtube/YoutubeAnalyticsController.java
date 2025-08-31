package org.example.ssj3pj.controller.youtube;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.DailyDemographicsDto;
import org.example.ssj3pj.dto.request.PeriodRequest;
import org.example.ssj3pj.dto.youtube.TrafficSourceCategoryDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.youtube.YoutubeAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * - POST /api/youtube/traffic-source-summary
 * - POST /api/youtube/daily-demographics
 */
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
public class YoutubeAnalyticsController {
    private final UsersRepository usersRepository;
    private final YoutubeAnalyticsService service;

    @PostMapping("/traffic-source-summary/{videoId}")
    public ResponseEntity<?> trafficSourceSummary(Principal principal, @PathVariable String videoId) {
        try {
            log.info("traffic-source-summary for videoId: {} by user: {}", videoId, principal.getName());
            
            // 1. 사용자 인증 및 조회
            String username = principal.getName();
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // 2~4. YoutubeMetadata 조회 → ES 검색 → 집계 처리 (Service에서 처리)
            List<TrafficSourceCategoryDto> data = service.trafficSourceByVideoId(user, videoId);
            
            log.info("traffic-source-summary 성공: user={}, videoId={}, categories={}", 
                    username, videoId, data.size());
            
            return ResponseEntity.ok(Map.of("status", 200, "message", "성공", "data", data));

        } catch (IllegalArgumentException e) {
            log.warn("traffic-source-summary 입력 오류: videoId={}, error={}", videoId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("traffic-source-summary 비즈니스 로직 오류: videoId={}", videoId, e);
            return ResponseEntity.internalServerError().body(Map.of("status", 500, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("traffic-source-summary 예상치 못한 오류: videoId={}", videoId, e);
            return ResponseEntity.internalServerError().body(Map.of("status", 500, "message", "Internal Server Error"));
        }
    }

    @PostMapping("/daily-demographics")
    public ResponseEntity<?> dailyDemographics(Principal principal, @RequestBody PeriodRequest req) {
        try {
            LocalDate startDate = LocalDate.parse(req.getStartDate());
            LocalDate endDate   = LocalDate.parse(req.getEndDate());
            log.info("daily-demographics: {} ~ {} by user: {}", startDate, endDate, principal.getName());

            // 1. 사용자 인증 및 조회
            String username = principal.getName();
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // 2~4. YoutubeMetadata 조회 → ES 검색 → 집계 처리 (Service에서 처리)
            List<DailyDemographicsDto> data = service.dailyDemographics(user, startDate, endDate);
            
            log.info("daily-demographics 성공: user={}, 기간={} ~ {}, records={}", 
                    username, startDate, endDate, data.size());
            
            return ResponseEntity.ok(Map.of("status", 200, "message", "성공", "data", data));

        } catch (DateTimeParseException e) {
            log.warn("daily-demographics 날짜 형식 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "날짜 형식 오류 (YYYY-MM-DD)"));
        } catch (IllegalArgumentException e) {
            log.warn("daily-demographics 입력 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("daily-demographics 비즈니스 로직 오류", e);
            return ResponseEntity.internalServerError().body(Map.of("status", 500, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("daily-demographics 예상치 못한 오류", e);
            return ResponseEntity.internalServerError().body(Map.of("status", 500, "message", "Internal Server Error"));
        }
    }
}
