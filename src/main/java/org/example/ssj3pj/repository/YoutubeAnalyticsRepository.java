package org.example.ssj3pj.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
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
 * 구현 레포만 사용(B옵션): ES 접근/파싱 전담
 * - 트래픽 소스 요약: terms + sum
 * - 일별 인구통계: 문서 조회 후 날짜별 그룹핑
 */
@Repository
@RequiredArgsConstructor
public class YoutubeAnalyticsRepository {

    private final ElasticsearchClient es;

    @Value("${app.es.idx.traffic:youtube_traffic_source_daily}")
    private String trafficIndex;

    @Value("${app.es.idx.demo:youtube_demographics_daily}")
    private String demoIndex;

    /** 기간 합산 트래픽 소스 요약 */
    public List<TrafficSourceDto> trafficSourceSummary(LocalDate start, LocalDate end) {
        try {
            List<Query> filters = List.of(
                    Query.of(q -> q.range(r -> r.field("stat_date")
                            .gte(JsonData.of(start.toString()))
                            .lte(JsonData.of(end.toString()))
                    ))
                    // 필요시 channel_id term 필터 추가 가능
            );

            SearchResponse<Void> res = es.search(s -> s
                            .index(trafficIndex)
                            .size(0) // 집계 전용
                            .query(q -> q.bool(b -> b.filter(filters)))
                            .aggregations("by_source", a -> a
                                    .terms(t -> t.field("insightTrafficSourceType").size(64))
                                    .aggregations("vsum", a2 -> a2.sum(su -> su.field("views")))
                            ),
                    Void.class
            );

            var buckets = res.aggregations().get("by_source").sterms().buckets().array();
            List<TrafficSourceDto> out = new ArrayList<>(buckets.size());
            for (var b : buckets) {
                Number v = b.aggregations().get("vsum").sum().value();
                out.add(TrafficSourceDto.builder()
                        .insightTrafficSourceType(b.key().stringValue())
                        .views(v == null ? 0L : v.longValue())
                        .build()
                );
            }
            // views desc 정렬
            out.sort(Comparator.comparingLong(TrafficSourceDto::getViews).reversed());
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
