package org.example.ssj3pj.mapper;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class EnvironmentSummaryMapper {

    @SuppressWarnings("unchecked")
    public EnvironmentSummaryDto fromEs(Map<String, Object> esDoc) {
        Map<String, Object> city = getMap(esDoc, "citydata");

        // 배열의 첫 요소만 사용 (실시간/현재값)
        Map<String, Object> ppl = firstMap(getList(city, "LIVE_PPLTN_STTS"));
        Map<String, Object> wth = firstMap(getList(city, "WEATHER_STTS"));

        String areaName        = s(or(city.get("AREA_NM"), v(ppl, "AREA_NM")));
        String congestionLevel = s(v(ppl, "AREA_CONGEST_LVL"));

        String temperature     = s(v(wth, "TEMP"));          // 예: "25.6"
        String humidity        = s(v(wth, "HUMIDITY"));      // 예: "89"
        String uvIndex         = s(v(wth, "UV_INDEX_LVL"));  // 숫자 레벨("5")을 사용. 텍스트면 "UV_INDEX"

        String maleRate   = s(v(ppl, "MALE_PPLTN_RATE"));
        String femaleRate = s(v(ppl, "FEMALE_PPLTN_RATE"));
        String teenRate   = s(v(ppl, "PPLTN_RATE_10"));
        String twentyRate = s(v(ppl, "PPLTN_RATE_20"));
        String thirtyRate = s(v(ppl, "PPLTN_RATE_30"));
        String fortyRate  = s(v(ppl, "PPLTN_RATE_40"));
        String fiftyRate  = s(v(ppl, "PPLTN_RATE_50"));
        String sixtyRate  = s(v(ppl, "PPLTN_RATE_60"));
        String seventyRate= s(v(ppl, "PPLTN_RATE_70"));

        return EnvironmentSummaryDto.builder()
                .areaName(areaName)
                .temperature(temperature)
                .humidity(humidity)
                .uvIndex(uvIndex)
                .congestionLevel(congestionLevel)
                .maleRate(maleRate)
                .femaleRate(femaleRate)
                .teenRate(teenRate)
                .twentyRate(twentyRate)
                .thirtyRate(thirtyRate)
                .fortyRate(fortyRate)
                .fiftyRate(fiftyRate)
                .sixtyRate(sixtyRate)
                .seventyRate(seventyRate)
                .build();
    }

    // ===== helpers =====
    private Object v(Map<String, Object> m, String key) { return m == null ? null : m.get(key); }
    private String s(Object o) { return o == null ? null : String.valueOf(o); }
    private Object or(Object a, Object b) { return a != null ? a : b; }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> m, String key) {
        Object o = m == null ? null : m.get(key);
        return (o instanceof Map) ? (Map<String, Object>) o : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> m, String key) {
        Object o = m == null ? null : m.get(key);
        return (o instanceof List) ? (List<Map<String, Object>>) o : Collections.emptyList();
    }

    private Map<String, Object> firstMap(List<Map<String, Object>> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : Collections.emptyMap();
    }
}
