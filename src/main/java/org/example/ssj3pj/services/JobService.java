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
    private final SseHub sseHub;

    // ✅ 새로 주입
    private final VideoRequestService videoRequestService;
    private final DynamicVideoScheduler dynamicVideoScheduler;

    @Transactional
    public Job createJobAndProcess(String imageKey, String locationCode, String purpose, String userName) {
        // 1. 사용자 조회
        Users user = usersRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        // 2. Job 생성 및 저장
        Job job = Job.builder()
                .user(user)
                .status("PROCESSING")
                .purpose(purpose)
                .locationCode(locationCode)
                .sourceImageKey(imageKey)
                .build();
        jobRepository.save(job);

        // 3. Redis에 jobId + userId + imageKey + locationCode 저장
        videoRequestService.saveJobRequest(job.getId(), user.getId(), imageKey, locationCode);

        // 4. 스케줄링 시작 (jobId 기준으로 관리, FastAPI에는 userId 전달)
        dynamicVideoScheduler.startJobSchedule(job.getId(), user.getId());

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
