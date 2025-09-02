package org.example.ssj3pj.services.Reddit;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.reddit.PostListDto;
import org.example.ssj3pj.dto.reddit.RDUploadRangeDto;
import org.example.ssj3pj.dto.youtube.*;
import org.example.ssj3pj.entity.RedditMetadata;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.example.ssj3pj.repository.RedditMetadataRepository;
import org.example.ssj3pj.services.ES.RedditQueryService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class RedditChannelService {

    private final ElasticsearchClient elasticsearchClient;
    private final RedditQueryService redditQueryService;
    private final RedditMetadataRepository redditMetadataRepository;


    public ChannelInfoDto getMyChannelInfo(Users user) {
        try {
            RedditMetadata metadata = redditMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("Reddit metadata not found for user: " + user));
            String esDocId = metadata.getEsDocId();
            ChannelInfoDto channelInfo = redditQueryService.findChannel(esDocId);
            return channelInfo;

        } catch (Exception e) {
            log.error("채널 정보 조회 실패 (ES): user={}", user, e);
            throw new RuntimeException("채널 정보 조회 실패: " + e.getMessage(), e);
        }
    }


    /**
     * 특정 채널의 비디오 목록 조회 (ES 기반, video_id별 최신 1건만)
     */
    public PostListDto getChannelPosts(Users user, String channelName) {
        try {
            log.info("채널 비디오 목록(중복 제거/최신 1개) 조회 시작: channelName={}", channelName);
            RedditMetadata metadata = redditMetadataRepository
                    .findFirstByUserAndChannelIdOrderByIndexedAtDesc(user, channelName)
                    .orElseThrow(() -> new RuntimeException(
                            "Reddit metadata not found for user: " + user.getUsername() + ", channelName: " + channelName));
            String esDocId = metadata.getEsDocId();
            PostListDto postList = redditQueryService.findAllPostForChannel(esDocId, channelName);
            return postList;

        } catch (Exception e) {
            log.error("채널 비디오 목록 조회 실패(중복 제거): channelName={}", channelName, e);
            throw new RuntimeException("채널 비디오 목록 조회 실패: " + e.getMessage(), e);
        }
    }

    /** 문자열/숫자 모두 안전하게 Long 변환 */
    private Long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (NumberFormatException ignore) {}
        }
        return 0L;
    }

    /** null-safe toString */
    private String str(Object v) {
        return v == null ? null : v.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractSdThumbnailUrl(String videoId, Map<String, Object> source) {
        try {
            Object thumbsObj = source.get("thumbnails");
            if (thumbsObj instanceof Map<?, ?>) {
                Map<String, Object> thumbs = (Map<String, Object>) thumbsObj;

                // YouTube Data API 표준 SD(640x480)
                Object stdObj = thumbs.get("standard");
                if (stdObj instanceof Map<?, ?>) {
                    Object url = ((Map<String, Object>) stdObj).get("url");
                    if (url instanceof String s && !s.isEmpty()) return s;
                }
                // 혹시 'sd'라는 커스텀 키를 쓰는 경우
                Object sdObj = thumbs.get("sd");
                if (sdObj instanceof Map<?, ?>) {
                    Object url = ((Map<String, Object>) sdObj).get("url");
                    if (url instanceof String s && !s.isEmpty()) return s;
                }
            }
            // 단일 필드가 sddefault면 사용
            Object single = source.get("thumbnail_url");
            if (single instanceof String s && s.contains("sddefault")) return s;
        } catch (Exception ignore) {}
        // 폴백: sddefault 강제
        return "https://i.ytimg.com/vi/" + videoId + "/sddefault.jpg";
    }

    @SuppressWarnings("unchecked")
    private String extractHqThumbnailUrl(String videoId, Map<String, Object> source) {
        try {
            Object thumbsObj = source.get("thumbnails");
            if (thumbsObj instanceof Map<?, ?>) {
                Map<String, Object> thumbs = (Map<String, Object>) thumbsObj;

                // high(480x360) 우선
                Object highObj = thumbs.get("high");
                if (highObj instanceof Map<?, ?>) {
                    Object url = ((Map<String, Object>) highObj).get("url");
                    if (url instanceof String s && !s.isEmpty()) return s;
                }
            }
            // 단일 필드가 hqdefault면 사용
            Object single = source.get("thumbnail_url");
            if (single instanceof String s && s.contains("hqdefault")) return s;
        } catch (Exception ignore) {}
        // 폴백: hqdefault 강제
        return "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
    }

    public RDUploadRangeDto uploadRange(Users user, LocalDate start, LocalDate end, String channelId) throws IOException{

        log.info("채널 비디오 목록(중복 제거/최신 1개) 조회 시작: channelId={}", channelId);
        RedditMetadata metadata = redditMetadataRepository
                .findFirstByUserAndChannelIdOrderByIndexedAtDesc(user, channelId)
                .orElseThrow(() -> new RuntimeException(
                        "Reddit metadata not found for user: " + user.getUsername() + ", channelId: " + channelId));
        String esDocId = metadata.getEsDocId();
        RDUploadRangeDto postList = redditQueryService.findAllPostRangeDate(esDocId, channelId, start, end);
        return postList;
    }
}
