package org.example.ssj3pj.controller;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.RedditSummaryDto;
import org.example.ssj3pj.services.RedditQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/es/reddit")
public class RedditController {

    private final RedditQueryService redditQueryService;

    @GetMapping("/{esDocId}/summary")
    public ResponseEntity<RedditSummaryDto> getSummary(@PathVariable String esDocId) {
        return ResponseEntity.ok(redditQueryService.getSummaryByDocId(esDocId));
    }
}
