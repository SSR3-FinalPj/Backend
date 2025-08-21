package org.example.ssj3pj.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeField;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeFieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.example.ssj3pj.dto.dashboard.DashboardDayStats;
import org.example.ssj3pj.dto.dashboard.DashboardRangeStats;
import org.example.ssj3pj.dto.dashboard.DashboardTotalStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

@Service
public class DashboardYoutubeService {

    private final ElasticsearchClient es;

    public DashboardYoutubeService(ElasticsearchClient es) {
        this.es = es;
    }

    /** 기본 인덱스: youtubedata (필요시 환경변수/인자로 app.es.index-youtube로 덮어쓰기 가능) */
    @Value("${app.es.index-youtube:youtubedata}")
    private String index;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 필드 상수 */
    private static final String F_VIDEO  = "video_id";      // ✅ ES 실제 필드명
    private static final String F_REGION = "region";
    private static final String F_CH     = "channel_id";
    private static final String F_TIME   = "upload_date";   // ✅ ES 실제 날짜 필드

    // 런타임 숫자 필드 이름 (실제 필드와 충돌 않도록 분리)
    private static final String F_VIEW_NUM = "view_count_l"; // ✅ runtime field
    private static final String F_LIKE_NUM = "like_count_l"; // ✅ runtime field

    @PostConstruct
    void logIndex() {
        System.out.println("[DashboardYoutubeService] ES index = " + index);
    }

    /* ① 단일 날짜 통계 - 스크립트/런타임 없이 자바에서 reduce */
    public DashboardDayStats dailyStats(LocalDate day,
                                        @Nullable String region,
                                        @Nullable String channelId) throws IOException {
        Instant start = day.atStartOfDay(KST).toInstant();
        Instant end   = day.plusDays(1).atStartOfDay(KST).toInstant();

        // 필터 쿼리 구성
        List<Query> filters = new ArrayList<>();
        filters.add(Query.of(q -> q.range(r -> r.field(F_TIME)
                .gte(JsonData.of(start.toString()))
                .lt(JsonData.of(end.toString()))
        )));
        if (notBlank(region)) {
            filters.add(Query.of(q -> q.term(t -> t.field(F_REGION).value(region))));
        }
        if (notBlank(channelId)) {
            filters.add(Query.of(q -> q.term(t -> t.field(F_CH).value(channelId))));
        }

        // 문서를 충분히 가져와서 자바에서 집계 (데이터 많아지면 scroll/search_after 고려)
        SearchResponse<Map> res = es.search(s -> s
                        .index(index)
                        .size(10_000)
                        .query(q -> q.bool(b -> b.filter(filters))),
                Map.class);

        // video_id별로 view/like의 "최대값"을 계산
        Map<String, long[]> perVideo = new HashMap<>(); // videoId -> [maxView, maxLike]
        for (var hit : res.hits().hits()) {
            Map src = hit.source();
            if (src == null) continue;

            Object vid = src.get("video_id"); // ✅ 필드명 수정
            if (vid == null) continue;
            String videoId = String.valueOf(vid);

            long v = parseLongSafe(src.get("view_count"));
            long l = parseLongSafe(src.get("like_count"));
            long c = parseLongSafe(src.get("comment_count"));

            long[] acc = perVideo.computeIfAbsent(videoId, k -> new long[]{0L, 0L, 0L});
            if (v > acc[0]) acc[0] = v;
            if (l > acc[1]) acc[1] = l;
            if (c > acc[2]) acc[2] = c;
        }

        long views = 0L, likes = 0L, comments = 0L;
        for (long[] p : perVideo.values()) {
            views += p[0];
            likes += p[1];
            comments += p[2]; //댓글 수 합계
        }

        return DashboardDayStats.builder()
                .date(day.toString())
                .viewCount(views)
                .likeCount(likes)
                .commentCount(comments)
                .build();
    }

    private static long parseLongSafe(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        String s = String.valueOf(val).trim();
        if (s.isEmpty()) return 0L;
        try { return Long.parseLong(s); } catch (Exception ignore) { return 0L; }
    }

    /* ② 기간별(일별 배열)  */
    public DashboardRangeStats rangeStats(LocalDate startDate,
                                              LocalDate endDate,
                                              @Nullable String region,
                                              @Nullable String channelId) throws IOException {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be >= startDate");
        }

        Instant start = startDate.atStartOfDay(KST).toInstant();
        Instant end   = endDate.plusDays(1).atStartOfDay(KST).toInstant();

        // 필터 쿼리 구성
        List<Query> filters = new ArrayList<>();
        filters.add(Query.of(q -> q.range(r -> r.field(F_TIME)
                .gte(JsonData.of(start.toString()))
                .lt(JsonData.of(end.toString()))
        )));
        if (notBlank(region)) {
            filters.add(Query.of(q -> q.term(t -> t.field(F_REGION).value(region))));
        }
        if (notBlank(channelId)) {
            filters.add(Query.of(q -> q.term(t -> t.field(F_CH).value(channelId))));
        }

        // 전체 기간의 문서를 가져와서 자바에서 날짜별로 그룹핑
        SearchResponse<Map> res = es.search(s -> s
                        .index(index)
                        .size(10_000) // 필요시 scroll 사용 고려
                        .query(q -> q.bool(b -> b.filter(filters))),
                Map.class);

        // 날짜별 + 비디오별로 그룹핑하여 최대값 계산
        Map<String, Map<String, long[]>> dayVideoStats = new HashMap<>(); // date -> videoId -> [maxView, maxLike]

        Map<String, long[]> totalVideoStats = new HashMap<>();

        for (var hit : res.hits().hits()) {
            Map src = hit.source();
            if (src == null) continue;

            Object timeObj = src.get(F_TIME);
            if (timeObj == null) continue;

            // 날짜 파싱
            LocalDate docDate;
            try {
                if (timeObj instanceof String) {
                    docDate = Instant.parse((String) timeObj).atZone(KST).toLocalDate();
                } else if (timeObj instanceof Number n) {
                    docDate = Instant.ofEpochMilli(n.longValue()).atZone(KST).toLocalDate();
                } else {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            Object vid = src.get(F_VIDEO);
            if (vid == null) continue;
            String videoId = String.valueOf(vid);

            long v = parseLongSafe(src.get("view_count"));
            long l = parseLongSafe(src.get("like_count"));
            long c = parseLongSafe(src.get("comment_count"));

            String dateKey = docDate.toString();
            Map<String, long[]> videoMap = dayVideoStats.computeIfAbsent(dateKey, k -> new HashMap<>());
            long[] acc = videoMap.computeIfAbsent(videoId, k -> new long[]{0L, 0L, 0L});
            if (v > acc[0]) acc[0] = v;
            if (l > acc[1]) acc[1] = l;
            if (c > acc[2]) acc[2] = c;

            long[] totalAcc = totalVideoStats.computeIfAbsent(videoId, k -> new long[]{0L, 0L, 0L});
            if (v > totalAcc[0]) totalAcc[0] = v;
            if (l > totalAcc[1]) totalAcc[1] = l;
            if (c > totalAcc[2]) totalAcc[2] = c;
        }

        long periodTotalViews = 0L;
        long periodTotalLikes = 0L;
        long periodTotalComments = 0L;
        for (long[] stats : totalVideoStats.values()) {
            periodTotalViews += stats[0];
            periodTotalLikes += stats[1];
            periodTotalComments += stats[2];
        }

        DashboardTotalStats totalStats = DashboardTotalStats.builder()
                .totalVideoCount(totalVideoStats.size())
                .totalViewCount(periodTotalViews)
                .totalLikeCount(periodTotalLikes)
                .totalCommentCount(periodTotalComments)
                .build();

        // 결과 생성 (모든 날짜 포함, 데이터 없는 날은 0으로)
        List<DashboardDayStats> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateKey = current.toString();
            long totalViews = 0L;
            long totalLikes = 0L;
            long totalComments = 0L;

            Map<String, long[]> videoMap = dayVideoStats.get(dateKey);
            if (videoMap != null) {
                for (long[] stats : videoMap.values()) {
                    totalViews += stats[0];
                    totalLikes += stats[1];
                    totalComments += stats[2];
                }
            }

            result.add(DashboardDayStats.builder()
                    .date(dateKey)
                    .viewCount(totalViews)
                    .likeCount(totalLikes)
                    .commentCount(totalComments)
                    .build());

            current = current.plusDays(1);
        }

        return DashboardRangeStats.builder()
                .total(totalStats)
                .daily(result)
                .build();
    }

    /* ③ 전체 누적 */
    public DashboardTotalStats totalStats(@Nullable String region,
                                          @Nullable String channelId) throws IOException {
        List<Query> filters = new ArrayList<>();
        if (notBlank(region)) {
            filters.add(Query.of(q -> q.term(t -> t.field(F_REGION).value(region))));
        }
        if (notBlank(channelId)) {
            filters.add(Query.of(q -> q.term(t -> t.field(F_CH).value(channelId))));
        }

        // 모든 문서를 가져와서 자바에서 집계
        SearchResponse<Map> res = es.search(s -> s
                        .index(index)
                        .size(10_000) // 필요시 scroll 사용 고려
                        .query(q -> q.bool(b -> {
                            if (!filters.isEmpty()) b.filter(filters);
                            return b;
                        })),
                Map.class);

        // video_id별로 view/like의 "최대값"을 계산
        Map<String, long[]> perVideo = new HashMap<>(); // videoId -> [maxView, maxLike, maxComment]
        for (var hit : res.hits().hits()) {
            Map src = hit.source();
            if (src == null) continue;

            Object vid = src.get("video_id");
            if (vid == null) continue;
            String videoId = String.valueOf(vid);

            long v = parseLongSafe(src.get("view_count"));
            long l = parseLongSafe(src.get("like_count"));
            long c = parseLongSafe(src.get("comment_count"));

            long[] acc = perVideo.computeIfAbsent(videoId, k -> new long[]{0L, 0L, 0L});
            if (v > acc[0]) acc[0] = v;
            if (l > acc[1]) acc[1] = l;
            if (c > acc[2]) acc[2] = c;
        }

        long totalViews = 0L;
        long totalLikes = 0L;
        long totalComments = 0L;
        for (long[] stats : perVideo.values()) {
            totalViews += stats[0];
            totalLikes += stats[1];
            totalComments += stats[2];
        }

        return DashboardTotalStats.builder()
                .totalVideoCount(perVideo.size()) // 유니크한 비디오 개수
                .totalViewCount(totalViews)
                .totalLikeCount(totalLikes)
                .totalCommentCount(totalComments)
                .build();
    }

    /* ===== 내부 유틸 ===== */

    private Map<String, RuntimeField> runtimeNumericMappings() {
        Map<String, RuntimeField> m = new HashMap<>();

        // view_count_l : _source에서 안전하게 파싱
        m.put(F_VIEW_NUM, RuntimeField.of(r -> r
                .type(RuntimeFieldType.Long)
                .script(s -> s.inline(i -> i
                        .lang("painless")
                        .source(
                                ""
                                        + "long out = 0L;"
                                        + "def v = params._source.containsKey('view_count') ? params._source.view_count : null;"
                                        + "if (v != null) {"
                                        + "  try { out = (v instanceof Number) ? ((Number)v).longValue() : Long.parseLong(v.toString()); }"
                                        + "  catch (e) { out = 0L; }"
                                        + "}"
                                        + "emit(out);"
                        )
                ))
        ));

        // like_count_l : _source에서 안전하게 파싱
        m.put(F_LIKE_NUM, RuntimeField.of(r -> r
                .type(RuntimeFieldType.Long)
                .script(s -> s.inline(i -> i
                        .lang("painless")
                        .source(
                                ""
                                        + "long out = 0L;"
                                        + "def v = params._source.containsKey('like_count') ? params._source.like_count : null;"
                                        + "if (v != null) {"
                                        + "  try { out = (v instanceof Number) ? ((Number)v).longValue() : Long.parseLong(v.toString()); }"
                                        + "  catch (e) { out = 0L; }"
                                        + "}"
                                        + "emit(out);"
                        )
                ))
        ));

        return m;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static long toLong(co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg) {
        if (agg == null || agg.max() == null) return 0L;
        Double v = agg.max().value();
        if (v == null || v.isNaN() || v.isInfinite()) return 0L;
        return v.longValue();
    }

    private <T> SearchResponse<T> safeSearch(
            Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn,
            Class<T> clazz
    ) throws IOException {
        try {
            return es.search(fn, clazz);
        } catch (ElasticsearchException e) {
            // 원인 로그 노출
            String reason = (e.response() != null && e.response().error() != null)
                    ? e.response().error().reason()
                    : e.getMessage();
            System.err.println("[ES SEARCH ERROR] " + reason);
            throw new IOException("Elasticsearch search failed: " + reason, e);
        }
    }
}
