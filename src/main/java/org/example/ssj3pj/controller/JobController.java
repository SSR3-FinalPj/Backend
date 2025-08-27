package org.example.ssj3pj.controller;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.JobService;
import org.example.ssj3pj.services.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final StorageService storage;
    private final JobService jobService;
    private final JwtUtils jwtUtils;

    // Request DTOs
    public record CreateJobRequest(@NotBlank String key, @NotBlank String locationCode) {}

    // Response DTOs
    public record CreateJobResponse(Long jobId, String status, String sourceImageKey) {}

    @PostMapping(path = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateJobResponse> createJob(@RequestBody CreateJobRequest req, HttpServletRequest request) {
        String userName = extractUserName(request);

        validateS3Key(req.key());
        String key = req.key();
        String pureKey = key.startsWith("images/") ? key.substring("images/".length()) : key;
        try {
            storage.head(req.key());

            Job job = jobService.createJobAndProcess(pureKey, req.locationCode(), "video", userName);

            CreateJobResponse response = new CreateJobResponse(job.getId(), job.getStatus(), job.getSourceImageKey());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (NoSuchKeyException e) {
            log.error("Source image not found in S3 for key: {}", pureKey, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source image not found in S3");
        } catch (Exception e) {
            log.error("Failed to create or process job for user: {}", userName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create or process job", e);
        }
    }



    private String extractUserName(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        try {
            return jwtUtils.getUserName(auth.substring(7));
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private void validateS3Key(String key) {
        if (key.contains("..") || key.startsWith("/") || key.contains("//")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid key format");
        }
    }
}