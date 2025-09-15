package org.example.ssj3pj.services.Reddit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.reddit.RedditUploadRequestDto;
import org.example.ssj3pj.dto.reddit.RedditUploadResultDto;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.SseHub;
import org.example.ssj3pj.services.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedditJobUploadService {

    private final RedditUploadService redditUploadService;  // Reddit API 호출
    private final StorageService storageService;
    private final JobResultRepository jobResultRepository;
    private final UsersRepository usersRepository;
    private final SseHub sseHub;

    /**
     * JobResult를 Reddit에 업로드
     */
    @Transactional
    public RedditUploadResultDto uploadJobResult(Long resultId,
                                                 RedditUploadRequestDto request,
                                                 Long userId) {
        Path tempFile = null;

        try {
            // 1. 권한 확인 및 데이터 조회
            JobResult jobResult = validateAndGetJobResult(resultId, userId);

            log.info("Reddit 업로드 시작: resultId={}, userId={}, subreddit={}, kind={}",
                    resultId, userId, request.getSubreddit(), jobResult.getType());

            // 2. S3에서 임시 파일 다운로드
            tempFile = storageService.downloadToTemporary(jobResult.getResultKey());

            String posterUrl = null;
            if ("video".equalsIgnoreCase(jobResult.getType())) {
                String sourceImageKey = jobResult.getJob().getSourceImageKey();  // jobs.source_image_key
                if (sourceImageKey != null && !sourceImageKey.isBlank()) {
                    posterUrl = storageService.getPublicUrl(sourceImageKey);
                }
            }
            // 3. Reddit 업로드 실행 (postId 반환)
            String postId = redditUploadService.uploadMediaPost(
                    userId,
                    request.getSubreddit(),
                    request.getTitle(),
                    tempFile.toFile(),
                    jobResult.getType(),
                    posterUrl
            );

            // ✅ 표준 Reddit URL (/r/{subreddit}/comments/{postId})
            String postUrl = "https://www.reddit.com/r/" + request.getSubreddit() + "/comments/" + postId;

            // 4. 성공 결과 생성
            RedditUploadResultDto result = RedditUploadResultDto.builder()
                    .success(true)
                    .postId(postId)
                    .postUrl(postUrl)
                    .title(request.getTitle())
                    .resultId(resultId)
                    .build();

            // 5. SSE 알림 발송
            sseHub.notifyRedditUploadCompleted(userId, postId);

            log.info("Reddit 업로드 완료: postId={}, url={}", postId, postUrl);
            return result;

        } catch (Exception e) {
            log.error("Reddit 업로드 실패: resultId={}", resultId, e);

            return RedditUploadResultDto.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .resultId(resultId)
                    .title(request.getTitle())
                    .build();

        } finally {
            // 6. 임시 파일 정리
            if (tempFile != null) {
                storageService.cleanupTemporaryFile(tempFile);
            }
        }
    }

    /**
     * 권한 확인 및 JobResult 조회
     */
    private JobResult validateAndGetJobResult(Long resultId, Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다"));

        JobResult jobResult = jobResultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "JobResult를 찾을 수 없습니다"));

        if (!"COMPLETED".equals(jobResult.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "완료되지 않은 작업 결과는 업로드할 수 없습니다");
        }

        if (!jobResult.getJob().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "해당 JobResult에 접근할 수 없습니다");
        }

        return jobResult;
    }
}
