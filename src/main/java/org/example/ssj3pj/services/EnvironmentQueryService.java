package org.example.ssj3pj.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnvironmentQueryService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    public EnvironmentSummaryDto getEnvironmentByDocId(String esDocId) {
        try {
            GetRequest request = new GetRequest.Builder()
                    .index("citydata")
                    .id(esDocId)
                    .build();

            GetResponse<JsonData> response = elasticsearchClient.get(request, JsonData.class);

            if (!response.found()) {
                throw new RuntimeException("❌ ES 문서 없음: " + esDocId);
            }

            JsonNode root = objectMapper.readTree(response.source().toJson().toString());

            JsonNode cityData = root.path("CITYDATA");
            JsonNode ppltn = cityData.path("LIVE_PPLTN_STTS").path("LIVE_PPLTN_STTS");
            JsonNode weather = cityData.path("WEATHER_STTS").path("WEATHER_STTS");

            return EnvironmentSummaryDto.builder()
                    .areaName(ppltn.path("AREA_NM").asText(null))
                    .congestionLevel(ppltn.path("AREA_CONGEST_LVL").asText(null))
                    .temperature(weather.path("TEMP").asText(null))
                    .humidity(weather.path("HUMIDITY").asText(null))
                    .uvIndex(weather.path("UV_INDEX").asText(null))
                    .maleRate(ppltn.path("MALE_PPLTN_RATE").asText(null))
                    .femaleRate(ppltn.path("FEMALE_PPLTN_RATE").asText(null))
                    .teenRate(ppltn.path("PPLTN_RATE_10").asText(null))
                    .twentyRate(ppltn.path("PPLTN_RATE_20").asText(null))
                    .thirtyRate(ppltn.path("PPLTN_RATE_30").asText(null))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("❌ ES 조회 또는 파싱 실패: " + e.getMessage(), e);
        }
    }
}
