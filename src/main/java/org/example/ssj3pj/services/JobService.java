package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.redis.VideoRequestService;
import org.example.ssj3pj.scheduler.DynamicVideoScheduler;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ✅ 새로 주입
    private final VideoRequestService videoRequestService;
    private final DynamicVideoScheduler dynamicVideoScheduler;

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

        // 3. Redis 저장 (스케줄러가 사용할 데이터)
        videoRequestService.saveUserRequest(job.getId(), user.getId(), imageKey, locationCode);

        // 4. 스케줄링 시작 (jobId를 esDocId처럼 사용)
        dynamicVideoScheduler.startJobSchedule(job.getId(), user.getId());

        // 5. Get environment summary from ES (즉시 1회 실행용)
        EnvironmentSummaryDto summary = environmentQueryService.getRecentSummaryByLocation(locationCode);
        if (summary == null) {
            throw new RuntimeException("No environment summary found for location code: " + locationCode);
        }

        log.info("ES END");
        // 6. Send data to FastAPI (AI service) with Job ID
        videoPromptSender.sendEnvironmentDataToFastAPI(summary, job.getId(), imageKey);

        return job;
    }

    @Transactional
    public JobResult completeJob(Job job, String resultKey, String resultType) {
        JobResult jobResult = JobResult.builder()
                .job(job)
                .resultKey(resultKey)
                .resultType(resultType)
                .status("COMPLETED")
                .build();
        jobResultRepository.save(jobResult);

        job.setStatus("COMPLETED");
        jobRepository.save(job);

        sseHub.notifyVideoReady(job.getId());

        return jobResult;
    }
}
