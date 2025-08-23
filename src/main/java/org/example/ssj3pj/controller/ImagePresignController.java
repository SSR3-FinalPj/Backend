package org.example.ssj3pj.controller;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.security.jwt.JwtUtils;
import org.example.ssj3pj.services.ImageUploadService;
import org.example.ssj3pj.services.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImagePresignController {

    private final StorageService storage;
    private final UsersRepository usersRepository;
    private final JwtUtils jwtUtils;

    @PostMapping(path = "/presign",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> presign(@RequestBody PresignReq req,
                                       HttpServletRequest request) {
        // 1) Authorization: Bearer <token> 에서 토큰 추출
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");

        String token = auth.substring(7);

        // 2) 토큰에서 userId(subject) 추출
        String userName;
        try {
            userName = jwtUtils.getUserName(token); // subject(userName)
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }

        Users user = usersRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userName));
        // 3) MIME 체크 및 확장자 결정
        if (!"image/png".equalsIgnoreCase(req.contentType())
                && !"image/jpeg".equalsIgnoreCase(req.contentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only image/png or image/jpeg allowed");
        }
        String ext = "image/jpeg".equalsIgnoreCase(req.contentType()) ? "jpg" : "png";

        // 4) 키 생성: images/{userId}/{yyyy}/{MM}/{dd}/{uuid}.{ext}
        LocalDate d = LocalDate.now();
        String key = String.format("images/%04d/%02d/%02d/%s.%s",
                d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                UUID.randomUUID(), ext);

        // 5) Presigned PUT URL 발급 (StorageService는 2-인자 버전)
        String url = storage.presignPut(key, req.contentType());

        return Map.of(
                "url", url,
                "key", key,
                "contentType", req.contentType()
        );
    }

    public record PresignReq(@NotBlank String contentType) {}

    private String sanitize(String s) {
        if (s == null || s.isBlank()) return "anon";
        // 영문/숫자/._- 만 허용
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
