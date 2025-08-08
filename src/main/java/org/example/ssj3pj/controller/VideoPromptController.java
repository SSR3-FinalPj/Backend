package org.example.ssj3pj.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.services.VideoPromptSender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


//  postman 테스트용
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/video")
public class VideoPromptController {

    private final VideoPromptSender videoPromptSender;

    @Tag(name = "prompt-data-sender", description = "프롬프트 데이터 전송")
    @PostMapping("/send/{esDocId}")
    public ResponseEntity<String> sendPrompt(@PathVariable String esDocId) {
        videoPromptSender.sendEnvironmentDataToFastAPI(esDocId);
        return ResponseEntity.ok("✅ FastAPI 전송 요청 완료");
    }
}
    