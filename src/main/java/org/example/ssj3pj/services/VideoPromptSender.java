package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class VideoPromptSender {

    private final EnvironmentQueryService environmentQueryService; // ES -> DTO
    private final RestTemplate restTemplate;
    private final UsersRepository usersRepository;                 // ★ 추가

    @Value("${BRIDGE_BASE_URL}")
    private String bridgeBaseUrl;

    /**
     * ES 문서 ID로 조회한 환경 요약 정보를 브릿지(FastAPI)로 전송
     * - 로그인한 사용자의 users.id를 DTO에 포함하여 전송
     */
    public void sendEnvironmentDataToFastAPI(String esDocId) {
        // 0) 로그인 사용자 식별
        Long userId = getCurrentUserId(); // ★ 핵심

        // 1) ES에서 DTO 조회
        EnvironmentSummaryDto dto = environmentQueryService.getSummaryByDocId(esDocId);

        // 2) 로그인 userId 부착 (Lombok @Data 이므로 setter 사용 가능)
        dto.setUserId(userId); // ★ 핵심

        // 3) 브릿지 엔드포인트
        String url = bridgeBaseUrl + "/api/generate-prompts";

        // 4) 전송
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, dto, String.class);
            System.out.println("✅ Bridge 응답: " + response.getBody());
        } catch (Exception e) {
            System.out.println("❌ Bridge 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * SecurityContext에서 현재 로그인 사용자의 username을 얻고,
     * DB에서 Users를 조회해 id를 반환
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        String username;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            username = ud.getUsername();
        } else {
            // JWT 등에서 principal이 문자열(username)로 오는 경우
            username = String.valueOf(principal);
        }

        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + username));

        return user.getId();
    }
}
