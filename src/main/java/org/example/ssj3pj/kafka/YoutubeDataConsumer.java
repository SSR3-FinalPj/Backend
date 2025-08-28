package org.example.ssj3pj.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.repository.YoutubeMetadataRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class YoutubeDataConsumer {

    private final ObjectMapper objectMapper;
    private final YoutubeMetadataRepository youtubeMetadataRepository;
    private final UsersRepository usersRepository;

    @KafkaListener(
            topics = "${spring.topics.kafka.raw2}",
            groupId = "#{T(java.util.UUID).randomUUID().toString()}",
            properties = {
                    "spring.json.trusted.packages=*",
                    "spring.deserializer.value.delegate.class=org.apache.kafka.common.serialization.StringDeserializer",
                    "auto.offset.reset=earliest"
            }
    )
    public void consumeYoutubeData(String jsonMessage, Acknowledgment acknowledgment) {
        try {
            log.info("Received message from youtubedata topic: {}", jsonMessage);
            JsonNode jsonNode = objectMapper.readTree(jsonMessage);

            long userId = jsonNode.get("user_id").asLong();
            String esDocId = jsonNode.get("es_doc_id").asText();
            String channelId = jsonNode.get("channel_id").asText();
            LocalDateTime indexedAt = LocalDateTime.parse(jsonNode.get("indexed_at").asText(), DateTimeFormatter.ISO_DATE_TIME);

            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found for id: " + userId));

            YoutubeMetadata youtubeMetadata = YoutubeMetadata.builder()
                    .user(user)
                    .esDocId(esDocId)
                    .channelId(channelId)
                    .indexedAt(indexedAt)
                    .build();

            youtubeMetadataRepository.save(youtubeMetadata);
            log.info("Successfully saved YoutubeMetadata for user {} and channel {}", userId, channelId);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse youtubedata message", e);
        } catch (Exception e) {
            log.error("Error processing youtubedata message", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}

