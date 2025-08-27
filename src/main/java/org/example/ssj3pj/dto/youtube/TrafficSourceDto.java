package org.example.ssj3pj.dto.youtube;

import lombok.Builder;
import lombok.Getter;

/** 트래픽 소스 요약 한 행 */
@Getter
@Builder
public class TrafficSourceDto {
    private final String insightTrafficSourceType;
    private final long views;
}
