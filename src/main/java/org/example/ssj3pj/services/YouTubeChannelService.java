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
import org.example.ssj3pj.dto.youtube.VideoListDto;
import org.example.ssj3pj.dto.youtube.VideoItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YouTube 채널 관련 서비스
 * - ES에서 사용자 채널 정보 조회
 * - ES에서 채널별 비디오 목록 조회
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
            
            // ES 검색 쿼리 구성 - processed_for_user 필드로 검색
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
    
    /**
     * 특정 채널의 비디오 목록 조회 (ES 기반)
     * 
     * @param channelId 채널 ID
     * @return 비디오 목록
     */
    public VideoListDto getChannelVideos(String channelId) {
        try {
            log.info("채널 비디오 목록 조회 시작 (ES 기반): channelId={}", channelId);
            
            // ES 검색 쿼리 구성 - channel_id 필드로 검색
            Query termQuery = Query.of(q -> q
                    .term(t -> t
                            .field("channel_id")
                            .value(channelId)
                    )
            );
            
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(youtubeIndex)
                    .query(termQuery)
                    .size(1000) // 많은 비디오 조회
                    .sort(sort -> sort.field(f -> f.field("published_at").order(SortOrder.Desc))) // 최신순 정렬
                    .source(src -> src.filter(f -> f.includes("video_id", "title", "published_at", "thumbnail_url")))
            );
            
            // ES 검색 실행
            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);
            
            List<VideoItemDto> videoItems = new ArrayList<>();
            
            for (Hit<Map> hit : searchResponse.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    String videoId = (String) source.get("video_id");
                    String title = (String) source.get("title");
                    String publishedAt = (String) source.get("published_at");
                    String thumbnailUrl = (String) source.get("thumbnail_url");
                    
                    if (videoId != null && !videoId.isEmpty()) {
                        VideoItemDto videoItem = VideoItemDto.builder()
                                .videoId(videoId)
                                .title(title != null ? title : "")
                                .publishedAt(publishedAt != null ? publishedAt : "")
                                .thumbnail(thumbnailUrl != null ? thumbnailUrl : "")
                                .url("https://www.youtube.com/watch?v=" + videoId)
                                .build();
                        
                        videoItems.add(videoItem);
                    }
                }
            }
            
            log.info("채널 비디오 목록 조회 성공 (ES): channelId={}, 비디오 수={}", 
                    channelId, videoItems.size());
            
            return VideoListDto.builder()
                    .channelId(channelId)
                    .videos(videoItems)
                    .build();
                    
        } catch (Exception e) {
            log.error("채널 비디오 목록 조회 실패 (ES): channelId={}", channelId, e);
            throw new RuntimeException("채널 비디오 목록 조회 실패: " + e.getMessage(), e);
        }
    }
}
