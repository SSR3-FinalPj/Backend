package org.example.ssj3pj.controller;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.Image;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.ImageRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.ImageUploadService;
import org.example.ssj3pj.services.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageConfirmController {

    private final StorageService storage; // head(), presignGet() 사용
    private final ImageUploadService imageUploadService;
    private final JwtUtils jwtUtils;
    private final UsersRepository usersRepository;
    private final ImageRepository imageRepository;

    /** 프론트가 S3 PUT 완료 후 key + locationCode만 전달 */
    @PostMapping(path = "/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> confirm(@RequestBody ConfirmReq req, HttpServletRequest request) {
        // 1) 인증 (필요 없으면 이 블록 제거 가능)
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

            Users user = usersRepository.findByUsername(userName)
                    .orElseThrow(() -> new RuntimeException("User not found for ID: " + userName));

            imageUploadService.uploadImageAndProcess(viewUrl, req.locationCode, userName);
            // 편의상 메타정보까지 응답. 정말 최소만 원하면 ok/key/locationCode만 돌려도 됨.
            return Map.of(
                    "ok", true,
                    "key", req.key(),
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

    // 요청 바디: 오로지 key와 locationCode만
    public record ConfirmReq(@NotBlank String key, @NotBlank String locationCode) {}

    // ---- helpers ----
    private String extractUserName(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        try {
            return jwtUtils.getUserName(auth.substring(7)); // subject(userId)
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
    }
}
