package org.example.ssj3pj.services.Reddit;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.dashboard.*;
import org.example.ssj3pj.entity.RedditMetadata;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.RedditMetadataRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.ES.RedditQueryService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardRedditService {

    private final UsersRepository usersRepository;
    private final RedditMetadataRepository redditMetadataRepository;
    private final RedditQueryService redditQueryService;

    /* ① 단일 날짜 통계 - 스크립트/런타임 없이 자바에서 reduce */
    public DashboardRDRangeStats rangeStats(LocalDate startDay,
                                            LocalDate endDay,
                                            @Nullable String region,
                                            @Nullable String channelId,
                                            String username) throws IOException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for username: " + username));

        List<DashboardRDDayStats> daily = new ArrayList<>();
        DashboardRDDayStats lastAvailableStats = null;

        // startDay부터 endDay까지 순회
        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            RedditMetadata metadata = redditMetadataRepository
                    .findFirstByUserAndIndexedAtBetweenOrderByIndexedAtDesc(user, start, end)
                    .orElse(null);

            DashboardRDDayStats dayStats;
            if (metadata != null) {
                // 해당 날짜 데이터가 있으면 그대로 가져오기
                dayStats = redditQueryService.findDayStatForChannel(metadata.getEsDocId(), day);
                lastAvailableStats = dayStats; // 마지막 사용 가능한 데이터 저장
            } else {
                if (lastAvailableStats != null) {
                    // 이전 날짜 데이터가 있으면 그것을 재사용
                    dayStats = DashboardRDDayStats.builder()
                            .date(day)
                            .upvoteCount(lastAvailableStats.getUpvoteCount())
                            .commentCount(lastAvailableStats.getCommentCount())
                            .postCount(lastAvailableStats.getPostCount())
                            .upvoteRatio(lastAvailableStats.getUpvoteRatio())
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
                DashboardRDDayStats nextAvailable = null;
                for (int j = i + 1; j < daily.size(); j++) {
                    if (daily.get(j) != null) {
                        nextAvailable = daily.get(j);
                        break;
                    }
                }
                if (nextAvailable != null) {
                    // null이면 뒤의 데이터로 채움
                    daily.set(i, DashboardRDDayStats.builder()
                            .date(startDay.plusDays(i))
                            .upvoteCount(nextAvailable.getUpvoteCount())
                            .commentCount(nextAvailable.getCommentCount())
                            .postCount(nextAvailable.getPostCount())
                            .upvoteRatio(nextAvailable.getUpvoteRatio())
                            .build());
                } else {
                    // 전체가 null인 경우 0으로 초기화
                    daily.set(i, DashboardRDDayStats.builder()
                            .date(startDay.plusDays(i))
                            .upvoteCount(0)
                            .commentCount(0)
                            .postCount(0)
                            .upvoteRatio(1.0)
                            .build());
                }
            }
        }

        // 전체 stats는 기존 로직
        RedditMetadata metadata = redditMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Reddit metadata not found for user: " + user));
        String esDocId = metadata.getEsDocId();
        DashboardRDTotalStats dashboardTotalStats = redditQueryService.findAllStat(esDocId);

        return DashboardRDRangeStats.builder()
                .total(dashboardTotalStats)
                .daily(daily)
                .build();
    }
    /* ③ 전체 누적 */
    public DashboardRDTotalStats totalStats(String username, String region, String channelId) throws IOException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for username: " + username));
        RedditMetadata metadata = redditMetadataRepository.findFirstByUserOrderByIndexedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + user));
        String esDocId = metadata.getEsDocId();
        DashboardRDTotalStats dashboardTotalStats = redditQueryService.findAllStat(esDocId);

        return dashboardTotalStats;
    }
}
