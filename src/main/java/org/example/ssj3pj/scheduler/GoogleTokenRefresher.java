package org.example.ssj3pj.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.example.ssj3pj.services.GoogleTokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GoogleTokenRefresher {

  private final GoogleTokenRepository tokenRepo;
  private final GoogleTokenService googleTokenService; // getValidAccessToken 호출용

  // 예: 1분마다 만료 120초 이내 토큰을 미리 갱신
  @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT10S")
  public void refreshSoonExpiring() {
    var threshold = Instant.now().plusSeconds(120); // yml의 google.skew-seconds 써도 됨
    tokenRepo.findExpiringSoon(threshold).forEach(t -> {
      try {
        googleTokenService.getValidAccessToken(t.getUser().getId()); // 내부에서 refresh+카프카 발행
      } catch (Exception e) {
        // 로그만
        System.out.println("[token-refresh] failed userId=" + t.getUser().getId() + " : " + e.getMessage());
      }
    });
  }
}
