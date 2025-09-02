package org.example.ssj3pj.controller.reddit;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.dashboard.DashboardRDRangeStats;
import org.example.ssj3pj.dto.dashboard.DashboardRDTotalStats;
import org.example.ssj3pj.dto.dashboard.DashboardYTRangeStats;
import org.example.ssj3pj.dto.dashboard.DashboardYTTotalStats;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.Reddit.DashboardRedditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/dashboard/reddit")
@RequiredArgsConstructor
public class DashboardRedditController {

    private final DashboardRedditService svc;
    private final JwtUtils jwtUtils;

    // ② 기간(일별 배열)
    @Tag(name = "dashboard", description = "대쉬보드")
    @GetMapping("/range")
    public ResponseEntity<DashboardRDRangeStats> daily(
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
    @Tag(name = "dashboard", description = "대쉬보드")
    @GetMapping("/total")
    public DashboardRDTotalStats total(
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
