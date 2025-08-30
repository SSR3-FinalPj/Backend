package org.example.ssj3pj.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.example.ssj3pj.repository.RedditTokenRepository;
import org.example.ssj3pj.services.Reddit.RedditTokenGuard;
import org.example.ssj3pj.services.google.GoogleTokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GoogleTokenRefresher {

  private final GoogleTokenRepository googleTokenRepo;
  private final GoogleTokenService googleTokenService;
  private final RedditTokenRepository redditTokenRepo;
  private final RedditTokenGuard redditTokenGuard;

  // 1분마다 만료 120초 이내 토큰 미리 갱신
  @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT10S")
  public void refreshSoonExpiring() {
    var threshold = Instant.now().plusSeconds(120);

    // 구글 토큰 갱신
    googleTokenRepo.findExpiringSoon(threshold).forEach(t -> {
      try {
        googleTokenService.getValidAccessToken(t.getUser().getId());
      } catch (Exception e) {
        System.out.println("[google-token-refresh] failed userId=" + t.getUser().getId() + " : " + e.getMessage());
      }
    });

    // 레딧 토큰 갱신
    redditTokenRepo.findExpiringSoon(threshold).forEach(t -> {
      try {
        redditTokenGuard.getValidAccessToken(t.getUser().getId());
      } catch (Exception e) {
        System.out.println("[reddit-token-refresh] failed userId=" + t.getUser().getId() + " : " + e.getMessage());
      }
    });
  }
}
