package org.example.ssj3pj.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.VideoDetailDto;
import org.example.ssj3pj.dto.youtube.VideoStatisticsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * YouTube 비디오 관련 서비스
 * - ES에서 단일 비디오 상세 정보 조회
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeVideoService {
    
    private final ElasticsearchClient elasticsearchClient;
    
    @Value("${app.es.indices.youtube:youtubedata}")
    private String youtubeIndex;
    
    /**
     * 특정 비디오의 상세 정보 조회 (ES 기반)
     * 
     * @param videoId YouTube 비디오 ID
     * @return 비디오 상세 정보
     */
    public VideoDetailDto getVideoDetail(String videoId) {
        try {
            log.info("비디오 상세 정보 조회 시작 (ES 기반): videoId={}", videoId);
            
            // ES 검색 쿼리 구성 - video_id 필드로 검색
            Query termQuery = Query.of(q -> q
                    .term(t -> t
                            .field("video_id")
                            .value(videoId)
                    )
            );
            
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(1)
                    .sort(sort -> sort.field(f -> f.field("published_at").order(SortOrder.Desc))) // 가장 최근 정보
                    .source(src -> src.filter(f -> f.includes(
                            "title", "published_at", "thumbnail_url", 
                            "view_count", "like_count", "comment_count"
                    )))
            );
            
            // ES 검색 실행
            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);
            
            if (searchResponse.hits().hits().isEmpty()) {
                throw new RuntimeException("존재하지 않는 비디오 ID: " + videoId);
            }
            
            // 첫 번째 검색 결과에서 데이터 추출
            Hit<Map> firstHit = searchResponse.hits().hits().get(0);
            Map<String, Object> source = firstHit.source();
            
            if (source == null) {
                throw new RuntimeException("ES 검색 결과에 _source 데이터가 없습니다.");
            }
            
            // 데이터 추출
            String title = (String) source.get("title");
            String publishedAt = (String) source.get("published_at");
            String thumbnailUrl = (String) source.get("thumbnail_url");
            
            // 통계 정보 추출 (Long으로 변환)
            Long viewCount = getLongValue(source.get("view_count"));
            Long likeCount = getLongValue(source.get("like_count"));
            Long commentCount = getLongValue(source.get("comment_count"));
            
            VideoStatisticsDto videoStatistics = VideoStatisticsDto.builder()
                    .viewCount(viewCount != null ? viewCount : 0L)
                    .likeCount(likeCount != null ? likeCount : 0L)
                    .commentCount(commentCount != null ? commentCount : 0L)
                    .build();
            
            VideoDetailDto videoDetail = VideoDetailDto.builder()
                    .title(title != null ? title : "")
                    .thumbnail(thumbnailUrl != null ? thumbnailUrl : "")
                    .publishedAt(publishedAt != null ? publishedAt : "")
                    .url("https://www.youtube.com/watch?v=" + videoId)
                    .statistics(videoStatistics)
                    .build();
            
            log.info("비디오 상세 정보 조회 성공 (ES): videoId={}, title={}", 
                    videoId, videoDetail.getTitle());
            
            return videoDetail;
            
        } catch (Exception e) {
            log.error("비디오 상세 정보 조회 실패 (ES): videoId={}", videoId, e);
            throw new RuntimeException("비디오 상세 정보 조회 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * Object를 Long으로 안전하게 변환
     * 
     * @param value 변환할 값
     * @return Long 값 또는 null
     */
    private Long getLongValue(Object value) {
        if (value == null) return null;
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("숫자 변환 실패: {}", value);
                return null;
            }
        }
        
        return null;
    }
}
