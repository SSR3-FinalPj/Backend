package org.example.ssj3pj.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.response.JobResultCreatedEvent;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.services.JobService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultConsumer {

    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobService jobService;

    @KafkaListener(topics = "video-callback", groupId = "${VIDEO_GENERATED_CONSUMER:video-generated-consumers-default}")
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            JobResultCreatedEvent event = objectMapper.readValue(message, JobResultCreatedEvent.class);
            log.info("Consumed job-result-created event: {}", event);
            Job job = jobRepository.findBySourceImageKey(event.getImageKey());

            // Delegate the logic to JobService
            jobService.completeJob(job, event.getResultKey(), event.getResultType());

            log.info("Successfully processed job result for job ID: {}", job.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse job-result-created event", e);
        } catch (RuntimeException e) {
            log.error("Error processing job-result-created event", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
