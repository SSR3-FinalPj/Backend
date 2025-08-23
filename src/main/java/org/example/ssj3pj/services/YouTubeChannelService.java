package org.example.ssj3pj.services;

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
     *
     * @param userId 사용자 ID
     * @return 채널 정보 (channel_id만 포함)
     */
    public ChannelInfoDto getMyChannelInfo(Long userId) {
        try {
            log.info("사용자 채널 정보 조회 시작 (ES 기반): userId={}", userId);
            log.info("사용 중인 Elasticsearch Index: {}", youtubeIndex);

            Query termQuery = Query.of(q -> q
                    .term(t -> t
                            .field("processed_for_user")
                            .value(userId.toString())
                    )
            );

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(1)
                    .source(src -> src.filter(f -> f.includes("channel_id")))
            );

            // ES 검색 실행
            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);

            if (searchResponse.hits().hits().isEmpty()) {
                throw new RuntimeException("해당 사용자의 YouTube 채널 데이터가 없습니다. userId: " + userId);
            }

            // 첫 번째 검색 결과에서 channel_id 추출
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
                    .channelTitle("") // 빈 문자열로 설정
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
     *
     * @param channelId  채널 ID
     * @param pageToken  페이지 토큰 (현재 미사용)
     * @param maxResults 최대 결과 수
     * @return 비디오 목록
     */
    public VideoListDto getChannelVideos(String channelId, String pageToken, Integer maxResults) {
        try {
            log.info("채널 비디오 목록(중복 제거/최신 1개) 조회 시작: channelId={}", channelId);

            int size = (maxResults != null ? maxResults : 20);

            // 채널 필터
            Query termQuery = Query.of(q -> q
                    .term(t -> t.field("channel_id.keyword").value(channelId))
            );

            // 1) 메인 검색: video_id 단위 collapse + 최신순 정렬
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(size)
                    // 최신 문서가 그룹 대표로 선택되도록 정렬
                    .sort(st -> st.field(f -> f.field("upload_date").order(SortOrder.Desc)))
                    .sort(st -> st.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                    // video_id별 1개만
                    .collapse(c -> c.field("video_id.keyword"))
                    .source(src -> src.filter(f -> f.includes(
                            "video_id", "title", "upload_date",
                            "view_count", "like_count", "comment_count",
                            "thumbnails"
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

                // 썸네일
                String thumbnailUrl = "";
                Object thumbsObj = src.get("thumbnails");
                if (thumbsObj instanceof Map<?, ?> thumbs) {
                    Object highObj = thumbs.get("high");
                    if (highObj instanceof Map<?, ?> high) {
                        Object url = ((Map<?, ?>) high).get("url");
                        if (url != null) thumbnailUrl = url.toString();
                    }
                }
                if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                    thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
                }

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

            // 2) 총 고유 영상 수(approx.) 구하기: cardinality(video_id.keyword)
            SearchRequest countReq = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(0)
                    .aggregations("unique_videos", a -> a.cardinality(c -> c.field("video_id.keyword")))
            );
            SearchResponse<Map> countRes = elasticsearchClient.search(countReq, Map.class);

            int totalUnique = 0;
            if (countRes.aggregations() != null && countRes.aggregations().get("unique_videos") != null
                    && countRes.aggregations().get("unique_videos").isCardinality()) {
                totalUnique = Math.toIntExact(
                        countRes.aggregations().get("unique_videos").cardinality().value()
                );
            }

            PageInfoDto pageInfo = PageInfoDto.builder()
                    .resultsPerPage(videoItems.size())
                    .totalResults(totalUnique) // 고유(video_id) 개수
                    .build();

            return VideoListDto.builder()
                    .channelId(channelId)
                    .videos(videoItems)
                    .nextPageToken(null) // search_after로 확장 가능
                    .pageInfo(pageInfo)
                    .build();

        } catch (Exception e) {
            log.error("채널 비디오 목록 조회 실패(중복 제거): channelId={}", channelId, e);
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
}
