package org.example.ssj3pj.controller;

import org.example.ssj3pj.dto.dashboard.DashboardDayStats;
import org.example.ssj3pj.dto.dashboard.DashboardRangeStats;
import org.example.ssj3pj.dto.dashboard.DashboardTotalStats;
import org.example.ssj3pj.services.youtube.DashboardYoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/dashboard/youtube")
@RequiredArgsConstructor
public class DashboardYoutubeController {

    private final DashboardYoutubeService svc;

    // ① 단일 날짜
    @GetMapping
    public ResponseEntity<DashboardDayStats> daily(
            @RequestParam String date,
            @RequestParam(required = false) String region,
            @RequestParam(name = "channel_id", required = false) String channelId
    ) throws IOException {
        try {
            LocalDate d = LocalDate.parse(date);
            return ResponseEntity.ok(svc.dailyStats(d, region, channelId));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ② 기간(일별 배열)
    @GetMapping("/range")
    public ResponseEntity<DashboardRangeStats> range(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String region,
            @RequestParam(name = "channel_id", required = false) String channelId
    ) throws IOException {
        try {
            LocalDate s = LocalDate.parse(startDate);
            LocalDate e = LocalDate.parse(endDate);
            return ResponseEntity.ok(svc.rangeStats(s, e, region, channelId));
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ③ 전체 누적
    @GetMapping("/total")
    public DashboardTotalStats total(
            @RequestParam(required = false) String region,
            @RequestParam(name = "channel_id", required = false) String channelId
    ) throws IOException {
        return svc.totalStats(region, channelId);
    }
}
