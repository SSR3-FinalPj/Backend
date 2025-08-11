package org.example.ssj3pj.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.springframework.stereotype.Service;

import static org.example.ssj3pj.util.JsonNodeUtils.*;  // ← util 정적 임포트

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentQueryService {

    private static final String INDEX = "citydata";

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    public EnvironmentSummaryDto getSummaryByDocId(String esDocId) {
        try {
            GetRequest request = new GetRequest.Builder()
                    .index(INDEX)
                    .id(esDocId)
                    .build();

            GetResponse<JsonData> response = elasticsearchClient.get(request, JsonData.class);
            if (!response.found()) {
                throw new RuntimeException("❌ ES 문서 없음: " + esDocId);
            }

            JsonNode root = objectMapper.readTree(response.source().toJson().toString());

            // citydata / CITYDATA 모두 대응
            JsonNode city = coalesce(root.path("citydata"), root.path("CITYDATA"));

            // 배열의 첫 요소 사용
            JsonNode ppl = first(coalesce(city.path("LIVE_PPLTN_STTS"), city.path("live_ppltn_stts")));
            JsonNode wth = first(coalesce(city.path("WEATHER_STTS"), city.path("weather_stts")));

            String areaName        = firstText(city, "AREA_NM", ppl, "AREA_NM");
            String congestionLevel = text(ppl, "AREA_CONGEST_LVL");

            String temperature     = text(wth, "TEMP");
            String humidity        = text(wth, "HUMIDITY");
            String uvIndex         = firstText(wth, "UV_INDEX_LVL", "UV_INDEX");

            String maleRate   = text(ppl, "MALE_PPLTN_RATE");
            String femaleRate = text(ppl, "FEMALE_PPLTN_RATE");
            String teenRate   = text(ppl, "PPLTN_RATE_10");
            String twentyRate = text(ppl, "PPLTN_RATE_20");
            String thirtyRate = text(ppl, "PPLTN_RATE_30");
            String fortyRate  = text(ppl, "PPLTN_RATE_40");
            String fiftyRate  = text(ppl, "PPLTN_RATE_50");
            String sixtyRate  = text(ppl, "PPLTN_RATE_60");
            String seventyRate= text(ppl, "PPLTN_RATE_70");

            EnvironmentSummaryDto dto = EnvironmentSummaryDto.builder()
                    .areaName(areaName)
                    .congestionLevel(congestionLevel)
                    .temperature(temperature)
                    .humidity(humidity)
                    .uvIndex(uvIndex)
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

            if (log.isDebugEnabled()) {
                log.debug("📦 DTO READY : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto));
            }
            return dto;

        } catch (Exception e) {
            throw new RuntimeException("❌ ES 조회 또는 파싱 실패: " + e.getMessage(), e);
        }
    }

    public String getRawSourceByDocId(String esDocId) {
        try {
            GetResponse<JsonData> response = elasticsearchClient.get(
                    new GetRequest.Builder().index(INDEX).id(esDocId).build(),
                    JsonData.class
            );
            if (!response.found()) throw new RuntimeException("❌ ES 문서 없음: " + esDocId);
            JsonNode root = objectMapper.readTree(response.source().toJson().toString());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("❌ ES 원본 조회 실패: " + e.getMessage(), e);
        }
    }
}
