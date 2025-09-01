package org.example.ssj3pj.services.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.request.YoutubeUploadRequestDto;
import org.example.ssj3pj.dto.response.YoutubeUploadResultDto;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.SseHub;
import org.example.ssj3pj.services.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

/**
 * JobResult를 YouTube에 업로드하는 통합 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeJobUploadService {

    private final YoutubeUploadService youtubeUploadService;
    private final StorageService storageService;
    private final JobRepository jobRepository;
    private final JobResultRepository jobResultRepository;
    private final UsersRepository usersRepository;
    private final SseHub sseHub;
    private final ObjectMapper objectMapper;

    /**
     * JobResult를 YouTube에 업로드
     */
    @Transactional
    public YoutubeUploadResultDto uploadJobResult(Long jobId, Long resultId, 
                                                 YoutubeUploadRequestDto request, Long userId) {
        Path tempVideoFile = null;
        
        try {
            // 1. 권한 확인 및 데이터 조회
            JobResult jobResult = validateAndGetJobResult(jobId, resultId, userId);
            
            log.info("YouTube 업로드 시작: jobId={}, resultId={}, userId={}", jobId, resultId, userId);
            
            // 2. S3에서 임시 파일로 다운로드
            tempVideoFile = storageService.downloadToTemporary(jobResult.getResultKey());
            
            // 3. YouTube 업로드 실행
            String youtubeResponse = youtubeUploadService.upload(
                userId,
                tempVideoFile.toString(),
                request.getTitle(),
                request.getDescription(),
                request.getTags(),
                request.getPrivacyStatus(),
                request.getCategoryId(),
                request.getMadeForKids()
            );
            
            // 4. 업로드 결과 파싱
            String videoId = extractVideoIdFromResponse(youtubeResponse);
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
            
            // 5. 성공 결과 생성
            YoutubeUploadResultDto result = YoutubeUploadResultDto.builder()
                    .success(true)
                    .videoId(videoId)
                    .videoUrl(videoUrl)
                    .title(request.getTitle())
                    .jobId(jobId)
                    .resultId(resultId)
                    .build();
                    
            // 6. SSE 알림 발송
            // sseHub.notifyYoutubeUploadCompleted(userId, videoId);
            
            log.info("YouTube 업로드 완료: videoId={}", videoId);
            return result;
            
        } catch (Exception e) {
            log.error("YouTube 업로드 실패: jobId={}, resultId={}", jobId, resultId, e);
            
            // 실패 결과 생성
            return YoutubeUploadResultDto.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .jobId(jobId)
                    .resultId(resultId)
                    .title(request.getTitle())
                    .build();
                    
        } finally {
            // 7. 임시 파일 정리
            if (tempVideoFile != null) {
                storageService.cleanupTemporaryFile(tempVideoFile);
            }
        }
    }

    /**
     * 권한 확인 및 JobResult 조회
     */
    private JobResult validateAndGetJobResult(Long jobId, Long resultId, Long userId) {
        // 사용자 존재 확인
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // Job 존재 확인
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job을 찾을 수 없습니다: " + jobId));

        // Job 소유권 확인
        if (!job.getUser().getId().equals(userId)) {
            throw new RuntimeException("Job에 대한 접근 권한이 없습니다");
        }

        // JobResult 존재 확인
        JobResult jobResult = jobResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("JobResult를 찾을 수 없습니다: " + resultId));

        // JobResult와 Job 연관 관계 확인
        if (!jobResult.getJob().getId().equals(jobId)) {
            throw new RuntimeException("JobResult가 지정된 Job에 속하지 않습니다");
        }

        // 완료된 결과만 업로드 가능
        if (!"COMPLETED".equals(jobResult.getStatus())) {
            throw new RuntimeException("완료되지 않은 작업 결과는 업로드할 수 없습니다");
        }

        return jobResult;
    }

    /**
     * YouTube API 응답에서 videoId 추출
     */
    private String extractVideoIdFromResponse(String youtubeResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(youtubeResponse);
            String videoId = jsonNode.path("id").asText();
            
            if (videoId == null || videoId.isEmpty()) {
                throw new RuntimeException("YouTube 응답에서 비디오 ID를 찾을 수 없습니다");
            }
            
            return videoId;
            
        } catch (Exception e) {
            throw new RuntimeException("YouTube 응답 파싱 실패", e);
        }
    }
}