package org.example.ssj3pj.services.youtube;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.DailyDemographicsDto;
import org.example.ssj3pj.dto.youtube.TrafficSourceDto;
import org.example.ssj3pj.dto.youtube.TrafficSourceCategoryDto;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.YoutubeMetadata;
import org.example.ssj3pj.repository.YoutubeMetadataRepository;
import org.example.ssj3pj.services.ES.YoutubeQueryService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ssj3pj.dto.youtube.DemographicPoint;

/**
 * YouTube Analytics 서비스 - 통일된 아키텍처 적용
 * 1. 사용자 인증 → 2. YoutubeMetadata 조회 → 3. ES 검색 → 4. 집계 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeAnalyticsService {

    private final YoutubeQueryService youtubeQueryService;
    private final YoutubeMetadataRepository youtubeMetadataRepository;
    private final ObjectMapper objectMapper;

    /** 단일 비디오의 트래픽 소스 조회 (카테고리별 그룹핑) */
    public List<TrafficSourceCategoryDto> trafficSourceByVideoId(Users user, String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("videoId must not be empty");
        }
        
        try {
            // 1. 사용자의 최신 YoutubeMetadata 조회
            YoutubeMetadata metadata = youtubeMetadataRepository
                    .findFirstByUserOrderByIndexedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + user.getUsername()));
            
            // 2. ES에서 트래픽 소스 데이터 조회 (esDocId 기반)
            List<TrafficSourceDto> rawData = youtubeQueryService
                    .findTrafficSourceByVideoId(metadata.getEsDocId(), videoId.trim());
            
            // 3. 카테고리별 매핑 및 합산
            Map<String, Long> categoryViews = new HashMap<>();
            
            for (TrafficSourceDto item : rawData) {
                String category = mapToCategory(item.getInsightTrafficSourceType());
                categoryViews.merge(category, item.getViews(), Long::sum);
            }
            
            // 4. DTO 변환 및 정렬 (조회수 내림차순)
            return categoryViews.entrySet().stream()
                    .map(entry -> TrafficSourceCategoryDto.builder()
                            .categoryCode(entry.getKey())
                            .categoryName(getCategoryName(entry.getKey()))
                            .totalViews(entry.getValue())
                            .build())
                    .sorted((a, b) -> Long.compare(b.getTotalViews(), a.getTotalViews()))
                    .collect(Collectors.toList());
            
        } catch (IOException e) {
            log.error("트래픽 소스 조회 실패: user={}, videoId={}", user.getUsername(), videoId, e);
            throw new RuntimeException("트래픽 소스 데이터 조회 실패: " + e.getMessage(), e);
        }
    }

    /** 기간별 트래픽 소스 요약 조회 */
    public List<TrafficSourceDto> trafficSourceSummaryByPeriod(Users user, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be >= startDate");
        }

        try {
            log.info("기간별 트래픽 소스 요약 조회 시작 : user={}, 기간={} ~ {}", user.getUsername(), startDate, endDate);

            Map<String, Long> totalTrafficViews = new HashMap<>();
            List<TrafficSourceDto> lastAvailableTraffic = new ArrayList<>();

            // 1. 기간 내 모든 날짜 반복
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                // 2. 각 날짜별 최신 문서 조회
                Optional<YoutubeMetadata> metadataOpt = youtubeMetadataRepository
                        .findFirstByUserAndIndexedAtBetweenOrderByIndexedAtDesc(user, date.atStartOfDay(), date.atTime(23, 59, 59));

                List<TrafficSourceDto> dataToProcess;

                if (metadataOpt.isPresent()) {
                    // 데이터가 있으면 사용하고, "마지막 데이터"로 저장
                    YoutubeMetadata metadata = metadataOpt.get();
                    log.info("날짜 {}에 대한 데이터 발견: esDocId={}", date, metadata.getEsDocId());
                    dataToProcess = youtubeQueryService.findTrafficSourceSummary(metadata.getEsDocId());
                    lastAvailableTraffic = dataToProcess;
                } else {
                    // 데이터가 없으면 "마지막 데이터"를 재사용
                    log.warn("날짜 {}에 대한 데이터 없음. 마지막 유효 데이터를 재사용합니다.", date);
                    dataToProcess = lastAvailableTraffic;
                }

                // 4. 해당 날짜의 데이터를 최종 합산
                if (dataToProcess != null && !dataToProcess.isEmpty()) {
                    for (TrafficSourceDto traffic : dataToProcess) {
                        totalTrafficViews.merge(traffic.getInsightTrafficSourceType(), traffic.getViews(), Long::sum);
                    }
                }
            }

            List<TrafficSourceDto> result = totalTrafficViews.entrySet().stream()
                    .map(entry -> TrafficSourceDto.builder()
                            .insightTrafficSourceType(entry.getKey())
                            .views(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            log.info("최종 트래픽 소스 계산 완료: {}개 항목", result.size());
            return result;

        } catch (IOException e) {
            log.error("기간별 트래픽 소스 조회 실패: user={}, startDate={}, endDate={}",
                    user.getUsername(), startDate, endDate, e);
            throw new RuntimeException("기간별 트래픽 소스 데이터 조회 실패: " + e.getMessage(), e);
        }
    }

    /** 사용자별 일별 인구통계 데이터 조회 */
    public List<DailyDemographicsDto> dailyDemographics(Users user, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be >= startDate");
        }
        
        try {
            log.info("일별 인구통계 조회 시작: user={}, 기간={} ~ {}", user.getUsername(), startDate, endDate);
            
            List<DailyDemographicsDto> result = new ArrayList<>();
            LocalDate currentDate = startDate;
            
            // 각 날짜별로 반복 처리
            while (!currentDate.isAfter(endDate)) {
                log.info("날짜별 처리: {}", currentDate);
                
                // 해당 날짜의 시작/끝 시간 설정
                LocalDateTime dayStart = currentDate.atStartOfDay();
                LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);
                
                // 해당 날짜에 수집된 최신 YoutubeMetadata 조회
                log.debug("메타데이터 조회 시도: 날짜범위 {} ~ {}", dayStart, dayEnd);
                Optional<YoutubeMetadata> metadata = youtubeMetadataRepository
                        .findFirstByUserAndIndexedAtBetweenOrderByIndexedAtDesc(user, dayStart, dayEnd);
                
                if (metadata.isPresent()) {
                    log.info("{}에 수집된 메타데이터 발견: esDocId={}, indexedAt={}", 
                            currentDate, metadata.get().getEsDocId(), metadata.get().getIndexedAt());
                    
                    // ES 문서 존재 여부 사전 확인
                    log.debug("ES 문서 조회 시도: esDocId={}", metadata.get().getEsDocId());
                    
                    // 해당 ES 문서에서 demographics 조회
                    List<DemographicPoint> demographics = youtubeQueryService
                            .getDemographicsFromES(metadata.get().getEsDocId());
                    
                    if (!demographics.isEmpty()) {
                        result.add(DailyDemographicsDto.builder()
                                .date(currentDate.toString())
                                .demographics(demographics)
                                .build());
                        
                        log.info("{}의 인구통계 데이터 추가: {} 개 항목", 
                                currentDate, demographics.size());
                    }
                } else {
                    log.warn("{}에 해당하는 메타데이터가 없음", currentDate);
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            log.info("일별 인구통계 조회 완료: {} 일간의 데이터", result.size());
            return result;
            
        } catch (IOException e) {
            log.error("인구통계 데이터 조회 실패: user={}, startDate={}, endDate={}", 
                    user.getUsername(), startDate, endDate, e);
            throw new RuntimeException("인구통계 데이터 조회 실패: " + e.getMessage(), e);
        }
    }

    
    /** 트래픽 소스를 카테고리로 매핑 */
    private String mapToCategory(String trafficSourceType) {
        if (trafficSourceType == null) return "OTHER";
        
        return switch (trafficSourceType.toUpperCase()) {
            // 1) 검색 (Search)
            case "YT_SEARCH" -> "SEARCH";
            
            // 2) 추천/탐색 (Discovery)
            case "RELATED_VIDEO", "END_SCREEN", "YT_OTHER_PAGE", "HASHTAGS" -> "DISCOVERY";
            
            // 3) 채널/구독/재생목록 (Owned Surfaces)
            case "SUBSCRIBER", "YT_CHANNEL", "PLAYLIST", "NOTIFICATION" -> "OWNED";
            
            // 4) 외부·직접/임베드 (External & Direct)
            case "EXT_URL", "NO_LINK_EMBEDDED", "NO_LINK_OTHER" -> "EXTERNAL";
            
            // 5) 기타 (Other)
            case "SHORTS", "ADVERTISING", "PROMOTED", "CAMPAIGN_CARD" -> "OTHER";
            
            // 미분류
            default -> "OTHER";
        };
    }
    
    /** 카테고리 코드를 한글명으로 변환 */
    private String getCategoryName(String categoryCode) {
        return switch (categoryCode) {
            case "SEARCH" -> "검색 (Search)";
            case "DISCOVERY" -> "추천/탐색 (Discovery)";
            case "OWNED" -> "채널/구독/재생목록 (Owned Surfaces)";
            case "EXTERNAL" -> "외부·직접/임베드 (External & Direct)";
            case "OTHER" -> "기타 (Other)";
            default -> "알 수 없음";
        };
    }
    
}
