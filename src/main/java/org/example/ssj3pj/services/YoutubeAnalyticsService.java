package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.DailyDemographicsDto;
import org.example.ssj3pj.dto.TrafficSourceDto;
import org.example.ssj3pj.dto.TrafficSourceCategoryDto;
import org.example.ssj3pj.repository.YoutubeAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

/** 얇은 서비스: 유효성 검사 + 레포 위임 */
@Service
@RequiredArgsConstructor
public class YoutubeAnalyticsService {

    private final YoutubeAnalyticsRepository repo;

    /** 단일 비디오의 트래픽 소스 조회 (카테고리별 그룹핑) */
    public List<TrafficSourceCategoryDto> trafficSourceByVideoId(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("videoId must not be empty");
        }
        
        // 1. 개별 트래픽 소스 데이터 가져오기
        List<TrafficSourceDto> rawData = repo.trafficSourceByVideoId(videoId.trim());
        
        // 2. 카테고리별 매핑 및 합산
        Map<String, Long> categoryViews = new HashMap<>();
        
        for (TrafficSourceDto item : rawData) {
            String category = mapToCategory(item.getInsightTrafficSourceType());
            categoryViews.merge(category, item.getViews(), Long::sum);
        }
        
        // 3. DTO 변환 및 정렬 (조회수 내림차순)
        return categoryViews.entrySet().stream()
                .map(entry -> TrafficSourceCategoryDto.builder()
                        .categoryCode(entry.getKey())
                        .categoryName(getCategoryName(entry.getKey()))
                        .totalViews(entry.getValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getTotalViews(), a.getTotalViews()))
                .collect(Collectors.toList());
    }

    public List<TrafficSourceDto> trafficSourceSummary(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new IllegalArgumentException("endDate must be >= startDate");
        return repo.trafficSourceSummary(start, end);
    }

    public List<DailyDemographicsDto> dailyDemographics(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new IllegalArgumentException("endDate must be >= startDate");
        return repo.dailyDemographics(start, end);
    }

    /** 트래픽 소스를 카테고리별로 그룹핑 */
    public List<TrafficSourceCategoryDto> trafficSourceByCategory(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new IllegalArgumentException("endDate must be >= startDate");
        
        // 1. 개별 트래픽 소스 데이터 가져오기
        List<TrafficSourceDto> rawData = repo.trafficSourceSummary(start, end);
        
        // 2. 카테고리별 매핑 및 합산
        Map<String, Long> categoryViews = new HashMap<>();
        
        for (TrafficSourceDto item : rawData) {
            String category = mapToCategory(item.getInsightTrafficSourceType());
            categoryViews.merge(category, item.getViews(), Long::sum);
        }
        
        // 3. DTO 변환 및 정렬 (조회수 내림차순)
        return categoryViews.entrySet().stream()
                .map(entry -> TrafficSourceCategoryDto.builder()
                        .categoryCode(entry.getKey())
                        .categoryName(getCategoryName(entry.getKey()))
                        .totalViews(entry.getValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getTotalViews(), a.getTotalViews()))
                .collect(Collectors.toList());
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
