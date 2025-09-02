package org.example.ssj3pj.services.youtube;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeField;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeFieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.example.ssj3pj.dto.dashboard.DashboardDayStats;
import org.example.ssj3pj.dto.dashboard.DashboardRangeStats;
import org.example.ssj3pj.dto.dashboard.DashboardTotalStats;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.repository.YoutubeMetadataRepository;
import org.example.ssj3pj.services.ES.YoutubeQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

import static org.apache.commons.lang3.Validate.notBlank;

@Service
@RequiredArgsConstructor
public class DashboardYoutubeService {

    private final ElasticsearchClient es;
    private final UsersRepository usersRepository;
    private final YoutubeMetadataRepository youtubeMetadataRepository;
    private final YoutubeQueryService youtubeQueryService;

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
    public DashboardRangeStats rangeStats(LocalDate startDay,
                                          LocalDate endDay,
                                          @Nullable String region,
                                          @Nullable String channelId,
                                          String username) throws IOException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for username: " + username));

        List<DashboardDayStats> daily = new ArrayList<>();
        DashboardDayStats lastAvailableStats = null;

        // startDay부터 endDay까지 순회
        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            YoutubeMetadata metadata = youtubeMetadataRepository
                    .findFirstByUserAndIndexedAtBetweenOrderByIndexedAtDesc(user, start, end)
                    .orElse(null);

            DashboardDayStats dayStats;
            if (metadata != null) {
                // 해당 날짜 데이터가 있으면 그대로 가져오기
                dayStats = youtubeQueryService.findDayStatForChannel(metadata.getEsDocId(), day);
                lastAvailableStats = dayStats; // 마지막 사용 가능한 데이터 저장
            } else {
                if (lastAvailableStats != null) {
                    // 이전 날짜 데이터가 있으면 그것을 재사용
                    dayStats = DashboardDayStats.builder()
                            .date(day)
                            .viewCount(lastAvailableStats.getViewCount())
                            .likeCount(lastAvailableStats.getLikeCount())
                            .commentCount(lastAvailableStats.getCommentCount())
                            .videoCount(lastAvailableStats.getVideoCount())
                            .subscriberCount(lastAvailableStats.getSubscriberCount())
                            .build();
                } else {
                    // 시작 날짜에 데이터가 없으면 다음 있는 날짜 데이터로 채워야 함
                    // 임시로 null 추가, 이후 채워질 예정
                    dayStats = null;
                }
            }
            daily.add(dayStats);
        }

        // 시작날짜가 null인 경우, 첫 번째 null을 찾고 뒤에서 채우기
        for (int i = 0; i < daily.size(); i++) {
            if (daily.get(i) == null) {
                // i번째 이후에서 첫 번째 null이 아닌 데이터 찾기
                DashboardDayStats nextAvailable = null;
                for (int j = i + 1; j < daily.size(); j++) {
                    if (daily.get(j) != null) {
                        nextAvailable = daily.get(j);
                        break;
                    }
                }
                if (nextAvailable != null) {
                    // null이면 뒤의 데이터로 채움
                    daily.set(i, DashboardDayStats.builder()
                            .date(startDay.plusDays(i))
                            .viewCount(nextAvailable.getViewCount())
                            .likeCount(nextAvailable.getLikeCount())
                            .commentCount(nextAvailable.getCommentCount())
                            .videoCount(nextAvailable.getVideoCount())
                            .subscriberCount(nextAvailable.getSubscriberCount())
                            .build());
                } else {
                    // 전체가 null인 경우 0으로 초기화
                    daily.set(i, DashboardDayStats.builder()
                            .date(startDay.plusDays(i))
                            .viewCount(0)
                            .likeCount(0)
                            .commentCount(0)
                            .videoCount(0)
                            .subscriberCount(0)
                            .build());
                }
            }
        }

        // 전체 stats는 기존 로직
        YoutubeMetadata metadata = youtubeMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + user));
        String esDocId = metadata.getEsDocId();
        DashboardTotalStats dashboardTotalStats = youtubeQueryService.findAllStat(esDocId);

        return DashboardRangeStats.builder()
                .total(dashboardTotalStats)
                .daily(daily)
                .build();
    }


    private static long parseLongSafe(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        String s = String.valueOf(val).trim();
        if (s.isEmpty()) return 0L;
        try {
            return Long.parseLong(s);
        } catch (Exception ignore) {
            return 0L;
        }
    }

    public DashboardDayStats dailyStats(LocalDate day,
                                        @Nullable String region,
                                        @Nullable String channelId) throws IOException {
        Instant start = day.atStartOfDay(KST).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(KST).toInstant();

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

        // 문서를 충분히 가져와서 자바에서 집계
        SearchResponse<Map> res = es.search(s -> s
                        .index(index)
                        .size(10_000)
                        .query(q -> q.bool(b -> b.filter(filters))),
                Map.class);

        // video_id별로 view/like/comment의 "최대값"을 계산
        Map<String, long[]> perVideo = new HashMap<>(); // videoId -> [maxView, maxLike, maxComment]
        for (var hit : res.hits().hits()) {
            Map src = hit.source();
            if (src == null) continue;

            Object vid = src.get("video_id"); // ✅ 필드명
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

        // 집계 합계 계산 (for문 밖에서)
        long views = 0L, likes = 0L, comments = 0L;
        for (long[] p : perVideo.values()) {
            views += p[0];
            likes += p[1];
            comments += p[2]; // 댓글 수 합계
        }

        return DashboardDayStats.builder()
                .date(day)
                .viewCount(views)
                .likeCount(likes)
                .commentCount(comments)
                .build(); // ✅ 누락된 build() 추가
    }

    /* ③ 전체 누적 */
    public DashboardTotalStats totalStats(String username, String region, String channelId) throws IOException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for username: " + username));
        YoutubeMetadata metadata = youtubeMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + user));
        String esDocId = metadata.getEsDocId();
        DashboardTotalStats dashboardTotalStats = youtubeQueryService.findAllStat(esDocId);

        return dashboardTotalStats;
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
