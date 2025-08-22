package org.example.ssj3pj.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvironmentSummaryDto {


    private Long userId;
    private String imagePath;
    // 위치 정보
    private String areaName;

    // 날씨 관련
    private String temperature;       // 온도
    private String humidity;          // 습도
    private String uvIndex;           // 자외선 지수

    // 혼잡도
    private String congestionLevel;   // 인구 혼잡도 (보통, 여유 등)

    // 인구 비율
    private String maleRate;
    private String femaleRate;
    private String teenRate;          // 10대
    private String twentyRate;        // 20대
    private String thirtyRate;        // 30대
    private String fortyRate;        // 40대
    private String fiftyRate;        // 50대
    private String sixtyRate;        // 60대
    private String seventyRate;        // 70대

}
