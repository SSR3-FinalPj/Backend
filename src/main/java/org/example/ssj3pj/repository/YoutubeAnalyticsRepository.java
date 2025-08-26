package org.example.ssj3pj.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.DailyDemographicsDto;
import org.example.ssj3pj.dto.DemographicPoint;
import org.example.ssj3pj.dto.TrafficSourceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * - 일별 인구통계: 문서 조회 후 날짜별 그룹핑
 */
@Repository
@RequiredArgsConstructor
public class YoutubeAnalyticsRepository {

    private final ElasticsearchClient es;
    private final ObjectMapper objectMapper;

    @Value("${app.es.idx.traffic:youtubedata}")
    private String trafficIndex;

    @Value("${app.es.idx.demo:youtubedata}")
    private String demoIndex;

    /** 단일 비디오 트래픽 소스 조회 - 해당 비디오의 최신 문서만 사용 */
    public List<TrafficSourceDto> trafficSourceByVideoId(String videoId) {
        try {
            // 특정 비디오의 최신 문서 가져오기
            SearchResponse<JsonData> docsRes = es.search(s -> s
                            .index(trafficIndex)
                            .size(1) // 최신 문서 1개만
                            .query(q -> q.term(t -> t.field("video_id.keyword").value(videoId)))
                            .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc))),
                    JsonData.class
            );

            List<TrafficSourceDto> result = new ArrayList<>();
            
            if (docsRes.hits().hits().isEmpty()) {
                return result; // 해당 비디오 데이터 없음
            }
            
            var latestDoc = docsRes.hits().hits().get(0);
            JsonData sourceData = latestDoc.source();
            if (sourceData == null) return result;
            
            try {
                // JsonData를 Map으로 변환
                String jsonString = sourceData.toJson().toString();
                Map<String, Object> source = objectMapper.readValue(jsonString, Map.class);
                
                // channel_analytics.traffic_source_analytics 배열 파싱
                Map<String, Object> channelAnalytics = (Map<String, Object>) source.get("channel_analytics");
                if (channelAnalytics == null) return result;
                
                List<Map<String, Object>> trafficAnalytics = (List<Map<String, Object>>) channelAnalytics.get("traffic_source_analytics");
                if (trafficAnalytics == null) return result;
                
                for (Map<String, Object> traffic : trafficAnalytics) {
                    String sourceType = (String) traffic.get("insightTrafficSourceType");
                    Object viewsObj = traffic.get("views");
                    
                    if (sourceType != null && viewsObj != null) {
                        long views = 0;
                        if (viewsObj instanceof Number) {
                            views = ((Number) viewsObj).longValue();
                        } else if (viewsObj instanceof String) {
                            try {
                                views = Long.parseLong((String) viewsObj);
                            } catch (NumberFormatException e) {
                                // 무시
                            }
                        }
                        
                        if (views > 0) { // 0인 것은 제외
                            result.add(TrafficSourceDto.builder()
                                    .insightTrafficSourceType(sourceType)
                                    .views(views)
                                    .build());
                        }
                    }
                }
            } catch (Exception e) {
                // JSON 파싱 실패 시 빈 리스트 반환
                return result;
            }
            
            // 조회수 내림차순 정렬
            result.sort((a, b) -> Long.compare(b.getViews(), a.getViews()));
            return result;

        } catch (IOException e) {
            throw new RuntimeException("ES single video traffic source failed", e);
        }
    }

    /** 기간 합산 트래픽 소스 요약 - 비디오별 최신 문서만 사용 */
    public List<TrafficSourceDto> trafficSourceSummary(LocalDate start, LocalDate end) {
        try {
            // 1단계: 비디오별로 최신 문서만 가져오기
            SearchResponse<JsonData> docsRes = es.search(s -> s
                            .index(trafficIndex)
                            .size(0)
                            .aggregations("by_video", a -> a
                                    .terms(t -> t.field("video_id.keyword").size(10000)) // 모든 비디오
                                    .aggregations("latest_doc", a2 -> a2
                                            .topHits(th -> th
                                                    .size(1) // 최신 문서 1개만
                                                    .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                                            )
                                    )
                            ),
                    JsonData.class
            );

            // 2단계: 최신 문서들에서 트래픽 소스 데이터 추출 및 합산
            Map<String, Long> trafficSources = new HashMap<>();
            
            var videoBuckets = docsRes.aggregations().get("by_video").sterms().buckets().array();
            for (var videoBucket : videoBuckets) {
                var topHits = videoBucket.aggregations().get("latest_doc").topHits();
                if (topHits.hits().hits().isEmpty()) continue;
                
                var latestDoc = topHits.hits().hits().get(0);
                JsonData sourceData = latestDoc.source();
                if (sourceData == null) continue;
                
                try {
                    // JsonData를 Map으로 변환
                    String jsonString = sourceData.toJson().toString();
                    Map<String, Object> source = objectMapper.readValue(jsonString, Map.class);
                    
                    // channel_analytics.traffic_source_analytics 배열 파싱
                    Map<String, Object> channelAnalytics = (Map<String, Object>) source.get("channel_analytics");
                    if (channelAnalytics == null) continue;
                    
                    List<Map<String, Object>> trafficAnalytics = (List<Map<String, Object>>) channelAnalytics.get("traffic_source_analytics");
                    if (trafficAnalytics == null) continue;
                    
                    for (Map<String, Object> traffic : trafficAnalytics) {
                        String sourceType = (String) traffic.get("insightTrafficSourceType");
                        Object viewsObj = traffic.get("views");
                        
                        if (sourceType != null && viewsObj != null) {
                            long views = 0;
                            if (viewsObj instanceof Number) {
                                views = ((Number) viewsObj).longValue();
                            } else if (viewsObj instanceof String) {
                                try {
                                    views = Long.parseLong((String) viewsObj);
                                } catch (NumberFormatException e) {
                                    // 무시
                                }
                            }
                            
                            trafficSources.merge(sourceType, views, Long::sum);
                        }
                    }
                } catch (Exception e) {
                    // JSON 파싱 실패 시 무시
                    continue;
                }
            }

            // 3단계: DTO 변환 및 정렬
            List<TrafficSourceDto> out = trafficSources.entrySet().stream()
                    .map(entry -> TrafficSourceDto.builder()
                            .insightTrafficSourceType(entry.getKey())
                            .views(entry.getValue())
                            .build())
                    .sorted((a, b) -> Long.compare(b.getViews(), a.getViews()))
                    .collect(Collectors.toList());
            
            return out;

        } catch (IOException e) {
            throw new RuntimeException("ES traffic summary failed", e);
        }
    }

    /** 일별 시청자 인구통계 */
    public List<DailyDemographicsDto> dailyDemographics(LocalDate start, LocalDate end) {
        try {
            SearchResponse<Map> res = es.search(s -> s
                            .index(demoIndex)
                            .size(10000) // 기간×그룹 수에 맞춰 조정
                            .query(q -> q.bool(b -> b
                                            .filter(f -> f.range(r -> r.field("stat_date")
                                                    .gte(JsonData.of(start.toString()))
                                                    .lte(JsonData.of(end.toString()))
                                            ))
                                    // 필요시 channel_id term 필터 추가 가능
                            ))
                            .sort(ss -> ss.field(f -> f.field("stat_date").order(SortOrder.Asc))),
                    Map.class
            );

            // 문서 스키마 가정: { stat_date, ageGroup, gender, viewerPercentage }
            Map<String, List<DemographicPoint>> byDate = new LinkedHashMap<>();
            for (var hit : res.hits().hits()) {
                Map<String, Object> src = hit.source();
                if (src == null) continue;

                String date = String.valueOf(src.get("stat_date"));
                String age  = String.valueOf(src.get("ageGroup"));
                String gen  = String.valueOf(src.get("gender"));
                double pct  = Double.parseDouble(String.valueOf(src.get("viewerPercentage")));

                byDate.computeIfAbsent(date, k -> new ArrayList<>())
                        .add(DemographicPoint.builder()
                                .ageGroup(age)
                                .gender(gen)
                                .viewerPercentage(pct)
                                .build());
            }

            return byDate.entrySet().stream()
                    .map(e -> DailyDemographicsDto.builder()
                            .date(e.getKey())
                            .demographics(e.getValue())
                            .build())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("ES demographics fetch failed", e);
        }
    }
}
