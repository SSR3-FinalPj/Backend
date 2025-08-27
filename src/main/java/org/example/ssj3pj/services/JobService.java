package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobResultRepository jobResultRepository;
    private final UsersRepository usersRepository;
    private final EnvironmentQueryService environmentQueryService;
    private final VideoPromptSender videoPromptSender;
    private final SseHub sseHub;

    @Transactional
    public Job createJobAndProcess(String imageKey, String locationCode, String purpose, String userName) {
        // 1. Find User
        Users user = usersRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userName));

        // 2. Create and save Job entity
        Job job = Job.builder()
                .user(user)
                .status("PROCESSING") // Immediately set to processing
                .purpose(purpose)
                .locationCode(locationCode)
                .sourceImageKey(imageKey)
                .build();
        jobRepository.save(job);

        // 3. Get environment summary from ES
        EnvironmentSummaryDto summary = environmentQueryService.getRecentSummaryByLocation(locationCode);
        if (summary == null) {
            throw new RuntimeException("No environment summary found for location code: " + locationCode);
        }

        log.info("ES END");
        // 4. Send data to FastAPI (AI service) with Job ID
        videoPromptSender.sendEnvironmentDataToFastAPI(summary, job.getId(), imageKey);

        return job;
    }

    @Transactional
    public JobResult completeJob(Job job, String resultKey, String resultType) {
        // 1. Find the original Job

        // 2. Create a new JobResult
        JobResult jobResult = JobResult.builder()
                .job(job)
                .resultKey(resultKey)
                .resultType(resultType)
                .status("COMPLETED")
                .build();
        jobResultRepository.save(jobResult);

        // 3. Update the parent Job's status
        job.setStatus("COMPLETED");
        jobRepository.save(job);

        // 4. Notify client via SSE
        sseHub.notifyJobCompleted(job.getUser().getId(), resultKey);

        return jobResult;
    }
}
