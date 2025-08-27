package org.example.ssj3pj.controller;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.redis.VideoRequestService;
import org.example.ssj3pj.repository.ImageRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.scheduler.DynamicVideoScheduler;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.ImageUploadService;
import org.example.ssj3pj.services.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageConfirmController {

    private final StorageService storage;
    private final ImageUploadService imageUploadService;
    private final JwtUtils jwtUtils;
    private final UsersRepository usersRepository;
    private final ImageRepository imageRepository;

    private final VideoRequestService videoRequestService;
    private final DynamicVideoScheduler dynamicVideoScheduler;

    @Value("${PROMPT_DEFAULT_ES_DOC_ID:latest}")
    private String defaultEsDocId;

    /** 프론트가 S3 PUT 완료 후 key + locationCode만 전달 */
    @PostMapping(path = "/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> confirm(@RequestBody ConfirmReq req, HttpServletRequest request) {
        // 1) 인증
        String userName = extractUserName(request);

        // 2) key 가드
        if (req.key().contains("..") || req.key().startsWith("/") || req.key().contains("//")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid key");
        }
        if (!req.key().startsWith("images/")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden key");
        }

        try {
            // 3) 실제 업로드 확인
            HeadObjectResponse head = storage.head(req.key());

            // (선택) 프리뷰용 GET URL
            String viewUrl = storage.presignGet(req.key(), head.contentType());

            String key = req.key();
            String pureKey = key.startsWith("images/") ? key.substring("images/".length()) : key;

            Users user = usersRepository.findByUsername(userName)
                    .orElseThrow(() -> new RuntimeException("User not found for ID: " + userName));

            // 4) 이미지key 저장
            imageUploadService.uploadImageAndProcess(pureKey, req.locationCode(), userName);

            // 5) Redis에 userId + imageKey + locationCode 저장
            videoRequestService.saveUserRequest(user.getId(), pureKey, req.locationCode());

            // 6) 동적 스케줄링 시작 (요청 시간 기준 → 1시간마다 → 18시에 stop)
            dynamicVideoScheduler.startUserSchedule(user.getId(), defaultEsDocId);

            return Map.of(
                    "ok", true,
                    "key", pureKey,
                    "locationCode", req.locationCode(),
                    "contentType", head.contentType(),
                    "size", head.contentLength(),
                    "etag", head.eTag(),
                    "uploaderId", user.getId(),
                    "viewUrl", viewUrl
            );
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "object not found");
        }
    }

    // 요청 바디: key + locationCode
    public record ConfirmReq(@NotBlank String key, @NotBlank String locationCode) {}

    private String extractUserName(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        try {
            return jwtUtils.getUserName(auth.substring(7));
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
    }
}
