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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

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
            
            log.info("[DEBUG] 사용자 메타데이터 조회: user={}, esDocId={}, channelId={}", 
                    user.getUsername(), metadata.getEsDocId(), metadata.getChannelId());
            
            // 2. ES에서 트래픽 소스 데이터 조회 (esDocId 기반)
            List<TrafficSourceDto> rawData = youtubeQueryService
                    .findTrafficSourceByVideoId(metadata.getEsDocId(), videoId.trim());
            
            log.info("[DEBUG] ES에서 반환된 원본 데이터: {} 개 항목", rawData.size());
            rawData.forEach(item -> log.info("[DEBUG] 원본: {} = {} views", 
                    item.getInsightTrafficSourceType(), item.getViews()));
            
            // 3. 카테고리별 매핑 및 합산
            Map<String, Long> categoryViews = new HashMap<>();
            
            for (TrafficSourceDto item : rawData) {
                String category = mapToCategory(item.getInsightTrafficSourceType());
                log.info("[DEBUG] 카테고리 매핑: {} → {}, views={}", 
                        item.getInsightTrafficSourceType(), category, item.getViews());
                categoryViews.merge(category, item.getViews(), Long::sum);
            }
            
            log.info("[DEBUG] 카테고리별 합산 결과:");
            categoryViews.forEach((category, views) -> 
                    log.info("[DEBUG] {} = {} views", category, views));
            
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

    /** 사용자별 일별 인구통계 데이터 조회 */
    public List<DailyDemographicsDto> dailyDemographics(Users user, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be >= startDate");
        }
        
        try {
            // 1. 사용자의 최신 YoutubeMetadata 조회
            YoutubeMetadata metadata = youtubeMetadataRepository
                    .findFirstByUserOrderByIndexedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("Youtube metadata not found for user: " + user.getUsername()));
            
            // 2. ES에서 인구통계 데이터 조회 (esDocId 기반)
            return youtubeQueryService.findDailyDemographics(metadata.getEsDocId(), startDate, endDate);
            
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
