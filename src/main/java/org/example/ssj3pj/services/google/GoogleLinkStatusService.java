package org.example.ssj3pj.services.google;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.GoogleLinkSimpleDto;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.example.ssj3pj.repository.GoogleTokenRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleLinkStatusService {

    private final GoogleTokenRepository tokenRepo;

    // 구글 연동됐는지 DB에서 Refreshtoken 존재 여부 확인
    public GoogleLinkSimpleDto getStatus(Long userId) {
        boolean linked = tokenRepo.findByUserId(userId)
                .map(GoogleToken::getRefreshToken)
                .map(rt -> !rt.isBlank())
                .orElse(false);

        return GoogleLinkSimpleDto.builder()
                .linked(linked)
                .build();
    }
}
