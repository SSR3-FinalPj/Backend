package org.example.ssj3pj.controller.youtube;

import jakarta.servlet.http.HttpServletRequest;
import org.example.ssj3pj.dto.dashboard.DashboardDayStats;
import org.example.ssj3pj.dto.dashboard.DashboardRangeStats;
import org.example.ssj3pj.dto.dashboard.DashboardTotalStats;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.youtube.DashboardYoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/dashboard/youtube")
@RequiredArgsConstructor
public class DashboardYoutubeController {

    private final DashboardYoutubeService svc;
    private final JwtUtils jwtUtils;

    // ② 기간(일별 배열)
    @GetMapping("/range")
    public ResponseEntity<DashboardRangeStats> daily(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String region,
            @RequestParam(name = "channel_id", required = false) String channelId,
            HttpServletRequest request
    ) throws IOException {
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
            LocalDate s = LocalDate.parse(startDate);
            LocalDate e = LocalDate.parse(endDate);
            return ResponseEntity.ok(svc.rangeStats(s, e, region, channelId, userName));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ③ 전체 누적
    @GetMapping("/total")
    public DashboardTotalStats total(
            HttpServletRequest request,
            @RequestParam(required = false) String region,
            @RequestParam(name = "channel_id", required = false) String channelId
    ) throws IOException {
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
        return svc.totalStats(userName, region, channelId);
    }
}
