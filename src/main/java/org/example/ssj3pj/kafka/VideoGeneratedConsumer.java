package org.example.ssj3pj.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.response.VideoGeneratedEvent;
import org.example.ssj3pj.entity.Image;
import org.example.ssj3pj.entity.Video;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.ImageRepository;
import org.example.ssj3pj.repository.VideoRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.SseHub;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoGeneratedConsumer {

    private final ObjectMapper objectMapper;
    private final ImageRepository imageRepository;
    private final VideoRepository videoRepository;
    private final UsersRepository usersRepository;
    private final SseHub sseHub;

    @KafkaListener(
        topics = "video-callback",
        groupId = "${VIDEO_GENERATED_CONSUMER:video-generated-consumers-default}"
        // , concurrency = "3" // 필요시 병렬 처리
    )
    public void onMessage(String rawJson) {
        try {
            VideoGeneratedEvent event = objectMapper.readValue(rawJson, VideoGeneratedEvent.class);

            // Fetch Image entity
            Image image = imageRepository.findByImageKey(event.getImageKey())
                    .orElseThrow(() -> new RuntimeException("Image not found for path: " + event.getImageKey()));

            // Fetch User entity
            Users user = usersRepository.findById(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found for ID: " + event.getUserId()));

            Video video = Video.builder()
                    .videoKey(event.getVideoKey())
                    .user(user)
                    .image(image)
                    .status(event.getStatus())
                    .promptText(event.getPrompt())
                    .build();
            sseHub.notifyVideoReady(event.getUserId());

            videoRepository.save(video);
            log.info("[KAFKA] Processed video.generated event for video: {}", event.getVideoKey());

        } catch (Exception e) {
            log.error("[KAFKA] Failed to process video.generated event: {}", rawJson, e);

        }
    }
}