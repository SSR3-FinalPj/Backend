package org.example.ssj3pj.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.JobResultDto;
import org.example.ssj3pj.dto.JobWithResultsDto;
import org.example.ssj3pj.dto.request.CommentUpdateJobRequest;
import org.example.ssj3pj.dto.request.CreateJobRequest;
import org.example.ssj3pj.dto.response.CreateJobResponse;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.JobService;
import org.example.ssj3pj.services.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final StorageService storage;
    private final JobService jobService;
    private final JobRepository jobRepository;
    private final UsersRepository usersRepository;
    private final JwtUtils jwtUtils;

    @PostMapping(path = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateJobResponse> createJob(@RequestBody CreateJobRequest req, HttpServletRequest request) {
        String userName = extractUserName(request);

        validateS3Key(req.key());
        String key = req.key();
        String pureKey = key.startsWith("images/") ? key.substring("images/".length()) : key;
        try {
            storage.head(req.key());

            Job job = jobService.createJobAndProcess(
                    pureKey,
                    req.locationCode(),
                    req.platform(),
                    userName,
                    req.prompt_text()
            );

            CreateJobResponse response = new CreateJobResponse(
                    job.getId(),
                    job.getStatus(),
                    job.getSourceImageKey(),
                    job.getPromptText()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (NoSuchKeyException e) {
            log.error("Source image not found in S3 for key: {}", pureKey, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source image not found in S3");
        } catch (Exception e) {
            log.error("Failed to create or process job for user: {}", userName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create or process job", e);
        }
    }

    @PostMapping(path = "/comments_update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateJobResponse> commentsUpdateJob(@RequestBody CommentUpdateJobRequest req, HttpServletRequest request) {
        String userName = extractUserName(request);

        Long resultId = req.result_id();
        JsonNode comments = req.comments();
        try {
//            storage.head(req.key());
//
//            Job job = jobService.createJobAndProcess(
//                    pureKey,
//                    req.locationCode(),
//                    req.platform(),
//                    userName,
//                    req.prompt_text()
//            );
//
//            CreateJobResponse response = new CreateJobResponse(
//                    job.getId(),
//                    job.getStatus(),
//                    job.getSourceImageKey(),
//                    job.getPromptText()
//            );
//            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            return null;
        } catch (NoSuchKeyException e) {
//            log.error("Source image not found in S3 for key: {}", pureKey, e);
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

    /** 특정 Job과 관련 결과들 조회 */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobWithResultsDto> getJobWithResults(@PathVariable Long jobId, HttpServletRequest request) {
        String userName = extractUserName(request);
        
        try {
            Users user = usersRepository.findByUsername(userName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다"));

            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job을 찾을 수 없습니다"));

            // 권한 확인 (본인의 Job만 조회 가능)
            if (!job.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
            }

            JobWithResultsDto result = JobWithResultsDto.fromEntity(job);
            return ResponseEntity.ok(result);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Job 조회 실패: jobId={}, user={}", jobId, userName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Job 조회 중 오류가 발생했습니다");
        }
    }

    /** 특정 Job의 결과 목록만 조회 */
    @GetMapping("/jobs/{jobId}/results")
    public ResponseEntity<List<JobResultDto>> getJobResults(@PathVariable Long jobId, HttpServletRequest request) {
        String userName = extractUserName(request);
        
        try {
            Users user = usersRepository.findByUsername(userName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다"));

            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job을 찾을 수 없습니다"));

            // 권한 확인
            if (!job.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
            }

            List<JobResultDto> results = job.getResults().stream()
                    .map(JobResultDto::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(results);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("JobResult 조회 실패: jobId={}, user={}", jobId, userName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JobResult 조회 중 오류가 발생했습니다");
        }
    }

    /** 현재 사용자의 모든 Job과 결과들 조회 */
    @GetMapping("/jobs/me")
    public ResponseEntity<List<JobWithResultsDto>> getMyJobs(HttpServletRequest request) {
        String userName = extractUserName(request);
        
        try {
            Users user = usersRepository.findByUsername(userName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다"));

            List<Job> userJobs = jobRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            
            List<JobWithResultsDto> results = userJobs.stream()
                    .map(JobWithResultsDto::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(results);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 Job 목록 조회 실패: user={}", userName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Job 목록 조회 중 오류가 발생했습니다");
        }
    }
}