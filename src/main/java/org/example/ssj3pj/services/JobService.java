package org.example.ssj3pj.services;

import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.redis.VideoRequestService;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.scheduler.DynamicVideoScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobResultRepository jobResultRepository;
    private final UsersRepository usersRepository;
    private final VideoRequestService videoRequestService;
    private final DynamicVideoScheduler dynamicVideoScheduler;
    private final SseHub sseHub;

    /**
     * 최초 요청
     */
    @Transactional
    public Job createInitialJob(String imageKey, String locationCode, String platform, String userName, String promptText) {
        Users user = usersRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        Job job = Job.builder()
                .user(user)
                .status("PROCESSING")
                .platform(platform)
                .locationCode(locationCode)
                .sourceImageKey(imageKey)
                .promptText(promptText)
                .build();

        jobRepository.save(job);

        videoRequestService.saveJobRequest(job.getId(), user.getId(), imageKey, locationCode, promptText, platform, true);
        dynamicVideoScheduler.startInitialJob(job.getId());

        return job;
    }

    /**
     * 수정 요청
     */
    @Transactional
    public Job createRevisionJob(Long resultId, String promptText, String userName) {
        Users user = usersRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        JobResult baseResult = jobResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Base result not found: " + resultId));

        Job job = Job.builder()
                .user(user)
                .status("PROCESSING")
                .platform(baseResult.getJob().getPlatform())
                .locationCode(baseResult.getJob().getLocationCode())
                .sourceImageKey(baseResult.getJob().getSourceImageKey())
                .promptText(promptText) // 새 프롬프트
                .parentResult(baseResult) // 이전 결과 기반
                .build();

        jobRepository.save(job);

        videoRequestService.saveJobRequest(job.getId(), user.getId(),
                job.getSourceImageKey(), job.getLocationCode(), promptText, job.getPlatform(), true);
        dynamicVideoScheduler.startRevisionJob(job.getId());

        return job;
    }

    @Transactional
    public JobResult completeJob(Job job, String resultKey, String type) {
        JobResult jobResult = JobResult.builder()
                .job(job)
                .resultKey(resultKey)
                .type(type)
                .status("COMPLETED")
                .build();

        jobResultRepository.save(jobResult);

        job.setStatus("COMPLETED");
        jobRepository.save(job);

        sseHub.notifyVideoReady(job.getId(), type);

        return jobResult;
    }
}
