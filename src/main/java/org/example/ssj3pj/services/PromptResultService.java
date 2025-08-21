package org.example.ssj3pj.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.response.VideoGeneratedEvent;
import org.example.ssj3pj.entity.PromptResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.PromptResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptResultService {

    private final PromptResultRepository promptResultRepo;
    private final UsersRepository usersRepo;
    private final ObjectMapper om;

    //AI 서버에서 영상 및 프롬프트 받아오기
    @Transactional
    public void upsertFromRawEvent(String rawJson) {
        try {
            VideoGeneratedEvent evt = om.readValue(rawJson, VideoGeneratedEvent.class);
            validate(evt);

            Users user = usersRepo.findById(evt.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + evt.getUserId()));

            PromptResult pr = promptResultRepo.findById(evt.getPromptId())
                    .orElseGet(() -> PromptResult.builder()
                            .promptId(evt.getPromptId())
                            .user(user)
                            .status("PENDING")  // 최초 기본값
                            .build());

            // 프롬프트/영상/상태/메시지 덮어쓰기 (요구사항: 최신만 유지)
            pr.setUser(user); // 기존과 달라도 최신 이벤트 기준으로 갱신
            pr.setPromptText(evt.getPrompt());
            pr.setVideoId(evt.getVideoId());
            pr.setVideoPath(evt.getVideo());
            pr.setStatus(evt.getStatus() == null || evt.getStatus().isBlank() ? "COMPLETED" : evt.getStatus());
            pr.setMessage(evt.getMessage());

            promptResultRepo.save(pr);

        } catch (Exception e) {
            log.error("Failed to upsert PromptResult from event: {}", rawJson, e);
            throw new RuntimeException(e);
        }
    }

    private void validate(VideoGeneratedEvent evt) {
        if (evt.getUserId() == null) throw new IllegalArgumentException("userId is required");
        if (evt.getPromptId() == null || evt.getPromptId().isBlank()) throw new IllegalArgumentException("promptId is required");
        if (evt.getPrompt() == null) throw new IllegalArgumentException("prompt is required");
        if (evt.getVideoId() == null || evt.getVideoId().isBlank()) throw new IllegalArgumentException("videoId is required");
        if (evt.getVideo() == null || evt.getVideo().isBlank()) throw new IllegalArgumentException("video (path/url) is required");
    }
}
