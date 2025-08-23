package org.example.ssj3pj.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.DailyDemographicsDto;
import org.example.ssj3pj.dto.request.PeriodRequest;
import org.example.ssj3pj.dto.TrafficSourceDto;
import org.example.ssj3pj.services.YoutubeAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private final YoutubeAnalyticsService service;

    @PostMapping("/traffic-source-summary")
    public ResponseEntity<?> trafficSourceSummary(@RequestBody PeriodRequest req) {
        try {
            LocalDate start = LocalDate.parse(req.getStartDate());
            LocalDate end   = LocalDate.parse(req.getEndDate());
            log.info("traffic-source-summary: {} ~ {}", start, end);

            List<TrafficSourceDto> data = service.trafficSourceSummary(start, end);
            return ResponseEntity.ok(Map.of("status", 200, "message", "성공", "data", data));

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "날짜 형식 오류 (YYYY-MM-DD)"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("traffic-source-summary 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("status", 500, "message", "Internal Server Error"));
        }
    }

    @PostMapping("/daily-demographics")
    public ResponseEntity<?> dailyDemographics(@RequestBody PeriodRequest req) {
        try {
            LocalDate start = LocalDate.parse(req.getStartDate());
            LocalDate end   = LocalDate.parse(req.getEndDate());
            log.info("daily-demographics: {} ~ {}", start, end);

            List<DailyDemographicsDto> data = service.dailyDemographics(start, end);
            return ResponseEntity.ok(Map.of("status", 200, "message", "성공", "data", data));

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "날짜 형식 오류 (YYYY-MM-DD)"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("daily-demographics 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("status", 500, "message", "Internal Server Error"));
        }
    }
}
