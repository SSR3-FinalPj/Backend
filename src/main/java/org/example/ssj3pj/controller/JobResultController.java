package org.example.ssj3pj.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.BothResultDto;
import org.example.ssj3pj.dto.BothUploadDto;
import org.example.ssj3pj.dto.dashboard.JobResultDto;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.youtube.JobResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class JobResultController {

    private final JwtUtils jwtUtils;
    private final JobResultService jobResultService;
    @Tag(name = "dashboard", description = "생성한 미디어 전체")
    @GetMapping("/result_id")
    public List<JobResultDto> getMyJobResults(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }

        String token = auth.substring(7);

        Long userId;
        try {
            userId = jwtUtils.getUidAsLong(token);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }

        return jobResultService.getUserJobResults(userId);
    }


    @Tag(name = "dashboard", description = "YouTube+Reddit 업로드 완료된 result_id 조회")
    @GetMapping("/result_id/both")
    public List<BothUploadDto> getResultIdsUploadedToBoth(HttpServletRequest request) throws IOException{
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }

        String token = auth.substring(7);
        Long userId;
        try {
            userId = jwtUtils.getUidAsLong(token);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }

        return jobResultService.getResultIdsUploadedToBoth(userId);
    }

    @Tag(name = "dashboard", description = "YouTube+Reddit 업로드 완료된 result_id 조회")
    @GetMapping("/both/{result_id}")
    public ResponseEntity<BothResultDto> getBothDatasFromResultId(HttpServletRequest request, @RequestParam Long resultId) throws IOException {
        log.info("일단 요청은 들어온듯");
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }
        String token = auth.substring(7);
        log.info("토큰도 정상인듯");
        String userName;
        try {
            userName = jwtUtils.getUserName(token);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }

        return ResponseEntity.ok(jobResultService.getBothDatasFromResultId(userName, resultId));
    }
}
