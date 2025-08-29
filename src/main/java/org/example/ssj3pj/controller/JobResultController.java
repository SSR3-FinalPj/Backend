package org.example.ssj3pj.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.dashboard.JobResultDto;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.youtube.JobResultService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

//
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class JobResultController {

    private final JwtUtils jwtUtils;
    private final JobResultService jobResultService;
    @Tag(name = "dashboard", description = "대쉬보드")
    @GetMapping("/result_id")
    public List<JobResultDto> getMyJobResults(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }

        String token = auth.substring(7);

        Long userId;
        try {
            userId = Long.valueOf(jwtUtils.getUidAsLong(token));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }

        return jobResultService.getUserJobResults(userId);
    }
}
