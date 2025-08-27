package org.example.ssj3pj.services.youtube;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.ChannelInfoDto;
import org.example.ssj3pj.dto.youtube.PageInfoDto;
import org.example.ssj3pj.dto.youtube.VideoDetailDto;
import org.example.ssj3pj.dto.youtube.VideoItemDto;
import org.example.ssj3pj.dto.youtube.VideoListDto;
import org.example.ssj3pj.dto.youtube.VideoStatisticsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YouTube 채널 관련 서비스
 * - ES에서 사용자 채널 정보 조회
 * - ES에서 채널별 비디오 목록 조회 (중복 제거: video_id별 최신 1개)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeChannelService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${app.es.indices.youtube:youtubedata}")
    private String youtubeIndex;

    /**
     * 사용자의 YouTube 채널 ID 조회 (ES 기반)
     */
    public ChannelInfoDto getMyChannelInfo(Long userId) {
        try {
            log.info("사용자 채널 정보 조회 시작 (ES 기반): userId={}", userId);
            log.info("사용 중인 Elasticsearch Index: {}", youtubeIndex);

            // processed_for_user 가 numeric 매핑인 케이스에 맞춰 숫자로 질의
            Query termQuery = Query.of(q -> q
                    .term(t -> t
                            .field("processed_for_user")
                            .value(userId)   // ← 숫자 그대로
                    )
            );

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(1)
                    .source(src -> src.filter(f -> f.includes("channel_id")))
            );

            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);

            if (searchResponse.hits().hits().isEmpty()) {
                throw new RuntimeException("해당 사용자의 YouTube 채널 데이터가 없습니다. userId: " + userId);
            }

            Hit<Map> firstHit = searchResponse.hits().hits().get(0);
            Map<String, Object> source = firstHit.source();
            if (source == null) {
                throw new RuntimeException("ES 검색 결과에 _source 데이터가 없습니다.");
            }

            String channelId = (String) source.get("channel_id");
            if (channelId == null || channelId.isEmpty()) {
                throw new RuntimeException("ES 데이터에 channel_id가 없습니다.");
            }

            log.info("채널 정보 조회 성공 (ES): userId={}, channelId={}", userId, channelId);

            return ChannelInfoDto.builder()
                    .channelId(channelId)
                    .channelTitle("")
                    .build();

        } catch (Exception e) {
            log.error("채널 정보 조회 실패 (ES): userId={}", userId, e);
            throw new RuntimeException("채널 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    public VideoListDto getChannelVideos(String channelId) {
        return getChannelVideos(channelId, null, 20);
    }

    /**
     * 특정 채널의 비디오 목록 조회 (ES 기반, video_id별 최신 1건만)
     */
    public VideoListDto getChannelVideos(String channelId, String pageToken, Integer maxResults) {
        try {
            log.info("채널 비디오 목록(중복 제거/최신 1개) 조회 시작: channelId={}", channelId);

            int size = (maxResults != null ? maxResults : 20);

            // 채널 필터
            Query termQuery = Query.of(q -> q
                    .term(t -> t.field("channel_id.keyword").value(channelId))
            );

            // video_id 그룹당 최신 1개만 선택 (collapse) + 최신 정렬
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(size)
                    .sort(st -> st.field(f -> f.field("upload_date").order(SortOrder.Desc)))
                    .sort(st -> st.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                    .collapse(c -> c.field("video_id.keyword"))
                    .source(src -> src.filter(f -> f.includes(
                            "video_id", "title", "upload_date",
                            "view_count", "like_count", "comment_count",
                            "thumbnails", "thumbnail_url"   // ← 단일 필드도 함께
                    )))
            );

            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);

            List<VideoItemDto> videoItems = new ArrayList<>();
            for (Hit<Map> hit : searchResponse.hits().hits()) {
                Map<String, Object> src = hit.source();
                if (src == null) continue;

                String videoId = str(src.get("video_id"));
                if (videoId == null || videoId.isEmpty()) continue;

                String title = str(src.get("title"));
                String publishedAt = str(src.get("upload_date"));

                Long viewCount = asLong(src.get("view_count"));
                Long likeCount = asLong(src.get("like_count"));
                Long commentCount = asLong(src.get("comment_count"));

                // 목록은 sddefault 정책
                String thumbnailUrl = extractSdThumbnailUrl(videoId, src);

                VideoStatisticsDto statistics = VideoStatisticsDto.builder()
                        .viewCount(viewCount)
                        .likeCount(likeCount)
                        .commentCount(commentCount)
                        .build();

                videoItems.add(
                        VideoItemDto.builder()
                                .videoId(videoId)
                                .title(title != null ? title : "")
                                .publishedAt(publishedAt != null ? publishedAt : "")
                                .thumbnail(thumbnailUrl)
                                .url("https://www.youtube.com/watch?v=" + videoId)
                                .statistics(statistics)
                                .build()
                );
            }

            log.info("중복 제거 결과: channelId={}, 반환 영상 수={}", channelId, videoItems.size());

            // 고유(video_id) 총 개수(근사)
            SearchRequest countReq = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(0)
                    .aggregations("unique_videos", a -> a.cardinality(c -> c.field("video_id.keyword")))
            );
            SearchResponse<Map> countRes = elasticsearchClient.search(countReq, Map.class);

            int totalUnique = 0;
            if (countRes.aggregations() != null
                    && countRes.aggregations().get("unique_videos") != null
                    && countRes.aggregations().get("unique_videos").isCardinality()) {
                totalUnique = Math.toIntExact(countRes.aggregations().get("unique_videos").cardinality().value());
            }

            PageInfoDto pageInfo = PageInfoDto.builder()
                    .resultsPerPage(videoItems.size())
                    .totalResults(totalUnique)
                    .build();

            return VideoListDto.builder()
                    .channelId(channelId)
                    .videos(videoItems)
                    .nextPageToken(null) // 필요시 search_after로 확장
                    .pageInfo(pageInfo)
                    .build();

        } catch (Exception e) {
            log.error("채널 비디오 목록 조회 실패(중복 제거): channelId={}", channelId, e);
            throw new RuntimeException("채널 비디오 목록 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 비디오 ID로 단일 영상 상세 정보 조회 (ES 기반, 최신 스냅샷 1건)
     */
    public VideoDetailDto getVideoDetail(String videoId) {
        try {
            log.info("단일 영상 상세 정보 조회 시작 (ES 기반): videoId={}", videoId);

            SearchResponse<Map> response = null;

            // 1) 정확 매칭: video_id.keyword
            try {
                Query keywordQuery = Query.of(q -> q
                        .term(t -> t.field("video_id.keyword").value(videoId))
                );

                SearchRequest keywordRequest = SearchRequest.of(s -> s
                        .index(youtubeIndex)
                        .query(keywordQuery)
                        .size(1)
                        .sort(st -> st.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                        .sort(st -> st.field(f -> f.field("upload_date").order(SortOrder.Desc)))
                        .source(src -> src.filter(f -> f.includes(
                                "title", "upload_date",
                                "view_count", "like_count", "comment_count",
                                "thumbnails", "thumbnail_url", "@timestamp"
                        )))
                );

                response = elasticsearchClient.search(keywordRequest, Map.class);
                log.info("keyword 필드 검색 결과: {} hits", response.hits().hits().size());
            } catch (Exception e) {
                log.warn("keyword 필드 검색 실패: {}", e.getMessage());
            }

            // 2) fallback: video_id (term)
            if (response == null || response.hits().hits().isEmpty()) {
                try {
                    Query termQuery = Query.of(q -> q
                            .term(t -> t.field("video_id").value(videoId))
                    );

                    SearchRequest termRequest = SearchRequest.of(s -> s
                            .index(youtubeIndex)
                            .query(termQuery)
                            .size(1)
                            .sort(st -> st.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                            .sort(st -> st.field(f -> f.field("upload_date").order(SortOrder.Desc)))
                            .source(src -> src.filter(f -> f.includes(
                                    "title", "upload_date",
                                    "view_count", "like_count", "comment_count",
                                    "thumbnails", "thumbnail_url", "@timestamp"
                            )))
                    );

                    response = elasticsearchClient.search(termRequest, Map.class);
                    log.info("일반 필드 검색 결과: {} hits", response.hits().hits().size());
                } catch (Exception e) {
                    log.warn("일반 필드 검색 실패: {}", e.getMessage());
                }
            }

            // 3) fallback: match(video_id)
            if (response == null || response.hits().hits().isEmpty()) {
                try {
                    Query matchQuery = Query.of(q -> q
                            .match(m -> m.field("video_id").query(videoId))
                    );

                    SearchRequest matchRequest = SearchRequest.of(s -> s
                            .index(youtubeIndex)
                            .query(matchQuery)
                            .size(1)
                            .sort(st -> st.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                            .sort(st -> st.field(f -> f.field("upload_date").order(SortOrder.Desc)))
                            .source(src -> src.filter(f -> f.includes(
                                    "title", "upload_date",
                                    "view_count", "like_count", "comment_count",
                                    "thumbnails", "thumbnail_url", "@timestamp"
                            )))
                    );

                    response = elasticsearchClient.search(matchRequest, Map.class);
                    log.info("match 쿼리 검색 결과: {} hits", response.hits().hits().size());
                } catch (Exception e) {
                    log.warn("match 쿼리 검색 실패: {}", e.getMessage());
                }
            }

            if (response == null || response.hits().hits().isEmpty()) {
                throw new RuntimeException("해당 비디오를 찾을 수 없습니다: " + videoId);
            }

            Map<String, Object> source = response.hits().hits().get(0).source();
            if (source == null) throw new RuntimeException("ES 검색 결과에 _source 데이터가 없습니다.");

            // 상세는 hqdefault 정책
            String thumbnailUrl = extractHqThumbnailUrl(videoId, source);

            String title       = str(source.get("title"));
            String publishedAt = str(source.get("upload_date"));
            Long viewCount     = asLong(source.get("view_count"));
            Long likeCount     = asLong(source.get("like_count"));
            Long commentCount  = asLong(source.get("comment_count"));

            VideoStatisticsDto statistics = VideoStatisticsDto.builder()
                    .viewCount(viewCount)
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .build();

            return VideoDetailDto.builder()
                    .title(title != null ? title : "")
                    .thumbnail(thumbnailUrl)
                    .publishedAt(publishedAt != null ? publishedAt : "")
                    .url("https://www.youtube.com/watch?v=" + videoId)
                    .statistics(statistics)
                    .build();

        } catch (Exception e) {
            log.error("단일 영상 상세 조회 실패 (ES): videoId={}", videoId, e);
            throw new RuntimeException("영상 상세 조회 실패: " + e.getMessage(), e);
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
}
