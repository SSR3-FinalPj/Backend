package org.example.ssj3pj.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.reddit.RedditContentDetailDto;
import org.example.ssj3pj.dto.youtube.YoutubeContentDetailDto;
import org.example.ssj3pj.entity.RedditMetadata;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.example.ssj3pj.repository.RedditMetadataRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.repository.YoutubeMetadataRepository;
import org.example.ssj3pj.services.ES.RedditQueryService;
import org.example.ssj3pj.services.ES.YoutubeQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 콘텐츠 조회 서비스
 * 다양한 타입의 콘텐츠에 대한 통합 조회 로직을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentsService {

    private final ElasticsearchClient elasticsearchClient;
    private final UsersRepository usersRepository;
    private final YoutubeMetadataRepository youtubeMetadataRepository;
    private final RedditMetadataRepository redditMetadataRepository;
    private final YoutubeQueryService youtubeQueryService;
    private final RedditQueryService redditQueryService;
    private final CommentSender commentSender;
    private final ObjectMapper objectMapper;

    @Value("${app.es.indices.youtube:youtubedata}")
    private String youtubeIndex;

    public JsonNode analyzeComments(String videoId, String username) throws IOException {
        // 1. Find user by username
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 2. Find the most recent es_doc_id for the user
        YoutubeMetadata metadata = youtubeMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + username));
        String esDocId = metadata.getEsDocId();

        // 3. Fetch all comments for the specific video from the ES document
        JsonNode videoComments = youtubeQueryService.findAllCommentsForVideo(esDocId, videoId);

        if (videoComments == null || videoComments.isEmpty()) {
            log.warn("No comments found for videoId: {} in docId: {}. Nothing to analyze.", videoId, esDocId);
            return objectMapper.createObjectNode(); // 빈 JSON 리턴
        }

        // 4. Wrap videoId and comments into youtube node
        JsonNode youtubeNode = objectMapper.createObjectNode()
                .put("videoId", videoId)
                .set("comments", videoComments);

        // 5. AI 서버에 요청 → 응답 JsonNode 반환
        return commentSender.sendCommentsToAi(videoComments);
    }

    public YoutubeContentDetailDto getContentDetailByVideoId(String videoId, String username) {
        try {
            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found for username: " + username));


            YoutubeMetadata metadata = youtubeMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + username));
            String esDocId = metadata.getEsDocId();

            YoutubeContentDetailDto videoDetail = youtubeQueryService.findAllDetailForVideo(esDocId, videoId);

            return videoDetail;
        } catch (Exception e) {
            log.error("콘텐츠 상세 조회 실패: videoId={}", videoId, e);
            throw new RuntimeException("콘텐츠 조회 실패: " + e.getMessage(), e);
        }
    }

    public RedditContentDetailDto getContentDetailByPostId(String postId, String username) throws IOException{
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for username: " + username));


        RedditMetadata metadata = redditMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Reddit metadata not found for user: " + username));
        String esDocId = metadata.getEsDocId();
        RedditContentDetailDto videoDetail = redditQueryService.findAllDetailForPost(esDocId, postId);

        return videoDetail;
    }
    public JsonNode analyzeRDComments(String postId, String username) throws IOException {
        // 1. Find user by username
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 2. Find the most recent es_doc_id for the user
        RedditMetadata metadata = redditMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Reddit metadata not found for user: " + username));
        String esDocId = metadata.getEsDocId();

        // 3. Fetch all comments for the specific video from the ES document
        JsonNode postComments = redditQueryService.findAllCommentsForPost(esDocId, postId);

        if (postComments == null || postComments.isEmpty()) {
            log.warn("No comments found for postId: {} in docId: {}. Nothing to analyze.", postId, esDocId);
            return objectMapper.createObjectNode(); // 빈 JSON 리턴
        }

        // 4. Wrap videoId and comments into youtube node
//        JsonNode redditNode = objectMapper.createObjectNode()
//                .put("postId", postId)
//                .set("comments", postComments);

        // 5. AI 서버에 요청 → 응답 JsonNode 반환
        return commentSender.sendCommentsToAi(postComments);
    }
    /**
     * ES Map 소스를 API 명세에 맞는 YoutubeContentDetailDto로 변환합니다.
     */
    private YoutubeContentDetailDto convertToContentDetailDto(String videoId, Map<String, Object> source) {
        // High 퀄리티 썸네일 URL 추출
        String thumbnailUrl = extractHighQualityThumbnail(source);
        
        return YoutubeContentDetailDto.builder()
                .videoId(videoId)
                .uploadDate(getStringValue(source, "upload_date"))
                .thumbnailUrl(thumbnailUrl)
                .title(getStringValue(source, "title"))
                .viewCount(getIntegerValue(source, "view_count"))
                .commentCount(getIntegerValue(source, "comment_count"))
                .likeCount(getIntegerValue(source, "like_count"))
                .build();
    }
    
    /**
     * ES 썸네일 데이터에서 High 퀄리티 URL 추출
     */
    private String extractHighQualityThumbnail(Map<String, Object> source) {
        try {
            Object thumbnailObj = source.get("thumbnail_url");
            if (thumbnailObj instanceof Map) {
                Map<?, ?> thumbnail = (Map<?, ?>) thumbnailObj;
                Object highObj = thumbnail.get("high");
                if (highObj instanceof Map) {
                    Map<?, ?> high = (Map<?, ?>) highObj;
                    Object url = high.get("url");
                    return url != null ? String.valueOf(url) : null;
                }
            }
        } catch (Exception e) {
            log.warn("썸네일 URL 추출 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Map에서 문자열 값 안전하게 추출
     */
    private String getStringValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value != null ? String.valueOf(value) : null;
    }
    
    /**
     * Map에서 정수 값 안전하게 추출
     */
    private Integer getIntegerValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("정수 변환 실패: {} = {}", key, value);
            return null;
        }
    }
}
