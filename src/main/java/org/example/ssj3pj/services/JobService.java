package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.ResultNodeDto;
import org.example.ssj3pj.dto.request.UserRequestData;
import org.example.ssj3pj.dto.response.JobResultCreatedEvent;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.redis.VideoRequestService;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.scheduler.DynamicVideoScheduler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 최초 요청
     */
    @Transactional
    public Job createInitialJob(String imageKey, String locationCode, String platform, String userName, String promptText, Long resultId, boolean city, String mascotImgKey) {
        Users user = usersRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));
        JobResult jobResult = null;
        if (resultId != null) {
            jobResult = jobResultRepository.getReferenceById(resultId);
        }

        Job job = Job.builder()
                .user(user)
                .status("PROCESSING")
                .platform(platform)
                .locationCode(locationCode)
                .sourceImageKey(imageKey)
                .promptText(promptText)
                .parentResult(jobResult)
                .useCitydata(city)
                .mascotImageKey(mascotImgKey)
                .build();
        jobRepository.save(job);

        videoRequestService.saveJobRequest(job.getId(), user.getId(), imageKey, locationCode, promptText, platform, true, 0);
        eventPublisher.publishEvent(new JobResultCreatedEvent(this, job.getId(), true));

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
                .promptText(promptText)
                .parentResult(baseResult)
                .build();
        jobRepository.save(job);

        videoRequestService.saveJobRequest(job.getId(), user.getId(),
                job.getSourceImageKey(), job.getLocationCode(), promptText, job.getPlatform(), false, 0);
        eventPublisher.publishEvent(new JobResultCreatedEvent(this, job.getId(), false));

        return job;
    }

    /**
     * 영상 생성 완료 처리
     */
    @Transactional
    public JobResult completeJob(Job job, String resultKey, String type) {
        JobResult jobResult = JobResult.builder()
                .job(job)
                .resultKey(resultKey)
                .type(type)
                .status("COMPLETED")
                .build();
        jobResultRepository.save(jobResult);

        UserRequestData data = videoRequestService.getJobRequest(job.getId());
        if (data == null) {
            log.warn("[COMPLETE] No redis data for job {}", job.getId());
            return jobResult;
        }

        int nextStep = data.getStep() + 1;
        data.setStep(nextStep);
        videoRequestService.saveJobRequest(
                data.getJobId(), data.getUserId(),
                data.getImageKey(), data.getLocationCode(),
                data.getPrompttext(), data.getPlatform(), data.isClient(), nextStep
        );

        if (nextStep < 4) {
            if (data.isClient()) {
                dynamicVideoScheduler.triggerNext(job.getId(), true, nextStep);
            } else {
                dynamicVideoScheduler.triggerNext(job.getId(), false, nextStep);
            }
        } else {
            job.setStatus("COMPLETED");
            jobRepository.save(job);
            sseHub.notifyVideoReady(job.getId(), type);
            log.info("[COMPLETE] Job {} is fully completed (4/4 results)", job.getId());
        }
        return jobResult;
    }

    /**
     * 결과 트리 빌드 (재귀)
     */
    @Transactional(readOnly = true)
    public ResultNodeDto buildResultTree(JobResult jobResult) {
        ResultNodeDto node = ResultNodeDto.builder()
                .resultId(jobResult.getId())
                .jobId(jobResult.getJob().getId())
                .type(jobResult.getType())
                .status(jobResult.getStatus())
                .resultKey(jobResult.getResultKey())
                .createdAt(jobResult.getCreatedAt())
                .children(new ArrayList<>())
                .build();

        List<Job> childJobs = jobRepository.findAllByParentResultId(jobResult.getId());
        for (Job childJob : childJobs) {
            List<JobResult> results = jobResultRepository.findAllByJobId(childJob.getId());
            for (JobResult jr : results) {
                node.getChildren().add(buildResultTree(jr));
            }
        }

        return node;
    }
}
