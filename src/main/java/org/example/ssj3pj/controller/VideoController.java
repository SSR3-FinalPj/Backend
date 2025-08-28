package org.example.ssj3pj.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final JwtUtils jwtUtils;
    private final JobResultRepository jobResultRepository;
    private final StorageService storage;

    @PostMapping("/stream")
    public Map<String, String> streamVideo(@RequestBody VideoReq req, HttpServletRequest request) {
        Long userId = extractUserId(request);

        var jobResult = jobResultRepository.findByIdAndJob_User_Id(req.resultId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "video not found for user"));

        String streamUrl = storage.presignGet(jobResult.getResultKey(), "video/mp4");

        return Map.of(
                "url", streamUrl,
                "expiresIn", "600"
        );
    }

    @PostMapping("/download")
    public Map<String, String> downloadVideo(@RequestBody VideoReq req, HttpServletRequest request) {
        Long userId = extractUserId(request);

        var jobResult = jobResultRepository.findByIdAndJob_User_Id(req.resultId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "video not found for user"));

        String downloadUrl = storage.presignDownload(jobResult.getResultKey());

        return Map.of(
                "url", downloadUrl,
                "expiresIn", "600"
        );
    }

    /**
     * JWT 토큰에서 userId 추출
     */
    private Long extractUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }
        String token = auth.substring(7);
        try {
            return jwtUtils.getUidAsLong(token);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
    }

    public record VideoReq(Long resultId) {}
}
