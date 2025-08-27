package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.reddit.RedditLinkSimpleDto;
import org.example.ssj3pj.repository.RedditTokenRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedditLinkStatusService {

    private final RedditTokenRepository repo;

    public RedditLinkSimpleDto getStatus(Long userId) {
        return repo.findByUserId(userId)
                .map(t -> new RedditLinkSimpleDto(true, t.getRedditUsername(), t.getExpiresAt()))
                .orElseGet(() -> new RedditLinkSimpleDto(false, null, null));
    }
}
