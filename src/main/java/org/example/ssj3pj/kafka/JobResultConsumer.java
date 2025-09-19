package org.example.ssj3pj.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.kafkamessage.MediaCallbackDto;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.services.JobService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultConsumer {

    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobService jobService;

    @KafkaListener(topics = "media-callback", groupId = "${VIDEO_GENERATED_CONSUMER:video-generated-consumers-default}")
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            MediaCallbackDto callbackDto = objectMapper.readValue(message, MediaCallbackDto.class);
            log.info("Consumed media callback: {}", callbackDto);
            Job job = jobRepository.findBySourceImageKey(callbackDto.getImageKey());

            if (job == null) {
                log.warn("Job not found for image key: {}", callbackDto.getImageKey());
                return; // or throw an exception
            }

            jobService.completeJob(job, callbackDto.getResultKey(), callbackDto.getType());

            log.info("Successfully processed media callback for job ID: {}", job.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse media callback message", e);
        } catch (RuntimeException e) {
            log.error("Error processing media callback message", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
