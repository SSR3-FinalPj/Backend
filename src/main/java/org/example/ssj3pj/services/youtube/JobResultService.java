package org.example.ssj3pj.services.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.BothResultDto;
import org.example.ssj3pj.dto.BothUploadDto;
import org.example.ssj3pj.dto.dashboard.JobResultDto;
import org.example.ssj3pj.dto.reddit.RedditContentDetailDto;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.RedditMetadata;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.repository.YoutubeMetadataRepository;
import org.example.ssj3pj.services.ContentsService;
import org.example.ssj3pj.services.ES.RedditQueryService;
import org.example.ssj3pj.services.ES.YoutubeQueryService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultService {

    private final JobResultRepository jobResultRepository;
    private final YoutubeMetadataRepository youtubeMetadataRepository;
    private final UsersRepository usersRepository;
    private final JobRepository jobRepository;
    private final YoutubeQueryService youtubeQueryService;
    private final ContentsService contentsService;

    public List<JobResultDto> getUserJobResults(Long userId) {
        return jobResultRepository.findAllByUserId(userId).stream()
                .map(r -> new JobResultDto(r.getId(), r.getCreatedAt()))
                .toList();
    }
    public List<JobResultDto> getUserRootNodes(Long userId) {
        List<Job> jobs = jobRepository.findAllByUserIdAndNoParent(userId);
        List<JobResultDto> resultDtos = new ArrayList<>();
        for (Job job : jobs){
            if (job.getResults() != null && !job.getResults().isEmpty()) {
                // id 기준으로 가장 작은 JobResult 선택
                job.getResults().stream()
                        .min(Comparator.comparing(JobResult::getId))
                        .ifPresent(firstResult ->
                                resultDtos.add(new JobResultDto(
                                        firstResult.getId(),
                                        firstResult.getCreatedAt()
                                ))
                        );
            }
        }
        return resultDtos;
    }
    public List<BothUploadDto> getResultIdsUploadedToBoth(Long userId) throws IOException{
        List<Long> jobResultIds = jobResultRepository.findResultIdsUploadedToBoth(userId);
        List<BothUploadDto> bothUploadDtos = new ArrayList<>();

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for userid: " + userId));
        YoutubeMetadata metadata = youtubeMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + userId));
        String esDocId = metadata.getEsDocId();

        for(Long jobResultId : jobResultIds){
            JobResult jobResult = jobResultRepository.findById(jobResultId)
                    .orElseThrow(() -> new RuntimeException("JobResult not found for id : " + jobResultId));
            String videoId = jobResult.getYtUpload();
            BothUploadDto bothUploadDto = youtubeQueryService.findDetailForVideo(esDocId, videoId);
            bothUploadDto.setResultId(jobResultId);
            bothUploadDto.setYoutube(jobResult.getYtUpload());
            bothUploadDto.setReddit(jobResult.getRdUpload());
            bothUploadDtos.add(bothUploadDto);
        }
        return bothUploadDtos;
    }
    public BothResultDto getBothDatasFromResultId(String userName, Long resultId) throws IOException {

        log.info("서비스까지 들어온듯");
        JobResult metadata = jobResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("JobResult metadata not found for user: " + userName));
        log.info("메타데이터도 정상인듯");
        String postId = metadata.getRdUpload();
        String videoId = metadata.getYtUpload();
        log.info("확실히 정상인듯");
        RedditContentDetailDto redditDetail = contentsService.getContentDetailByPostId(postId, userName);
        YoutubeContentDetailDto youtubeDetail = contentsService.getContentDetailByVideoId(videoId, userName);
        JsonNode aiRDResponse = contentsService.analyzeRDComments(postId, userName);
        JsonNode aiYTResponse = contentsService.analyzeComments(videoId, userName);
        log.info("위에껀 검증된건디");
        return BothResultDto.builder()
                .redditDetail(redditDetail)
                .youtubeDatil(youtubeDetail)
                .redditComments(aiRDResponse)
                .youtubeComments(aiYTResponse)
                .build();
    }

}
