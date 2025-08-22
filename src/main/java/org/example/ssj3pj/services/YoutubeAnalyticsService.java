package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.DailyDemographicsDto;
import org.example.ssj3pj.dto.TrafficSourceDto;
import org.example.ssj3pj.repository.YoutubeAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** 얇은 서비스: 유효성 검사 + 레포 위임 */
@Service
@RequiredArgsConstructor
public class YoutubeAnalyticsService {

    private final YoutubeAnalyticsRepository repo;

    public List<TrafficSourceDto> trafficSourceSummary(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new IllegalArgumentException("endDate must be >= startDate");
        return repo.trafficSourceSummary(start, end);
    }

    public List<DailyDemographicsDto> dailyDemographics(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new IllegalArgumentException("endDate must be >= startDate");
        return repo.dailyDemographics(start, end);
    }
}
