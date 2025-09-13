package org.example.ssj3pj.services.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.BothResultDto;
import org.example.ssj3pj.dto.dashboard.JobResultDto;
import org.example.ssj3pj.dto.reddit.RedditContentDetailDto;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.RedditMetadata;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.ContentsService;
import org.example.ssj3pj.services.ES.RedditQueryService;
import org.example.ssj3pj.services.ES.YoutubeQueryService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobResultService {

    private final JobResultRepository jobResultRepository;
    private final UsersRepository usersRepository;
    private final ContentsService contentsService;

    public List<JobResultDto> getUserJobResults(Long userId) {
        return jobResultRepository.findAllByUserId(userId).stream()
                .map(r -> new JobResultDto(r.getId(), r.getCreatedAt()))
                .toList();
    }
    public List<Long> getResultIdsUploadedToBoth(Long userId) {
        return jobResultRepository.findResultIdsUploadedToBoth(userId);
    }
    public BothResultDto getBothDatasFromResultId(String userName, Long resultId) throws IOException {
        JobResult metadata = jobResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("JobResult metadata not found for user: " + userName));
        String postId = metadata.getRdUpload();
        String videoId = metadata.getYtUpload();

        RedditContentDetailDto redditDetail = contentsService.getContentDetailByPostId(postId, userName);
        YoutubeContentDetailDto youtubeDetail = contentsService.getContentDetailByVideoId(videoId, userName);
        JsonNode aiRDResponse = contentsService.analyzeRDComments(postId, userName);
        JsonNode aiYTResponse = contentsService.analyzeComments(videoId, userName);
        return BothResultDto.builder()
                .redditDetail(redditDetail)
                .youtubeDatil(youtubeDetail)
                .redditComments(aiRDResponse)
                .youtubeComments(aiYTResponse)
                .build();
    }

}
