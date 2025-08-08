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
public class EnvironmentESService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    public EnvironmentSummaryDto getEnvironmentDataByDocId(String esDocId) {
        try {
            // 1. Elasticsearch 문서 조회 요청
            GetRequest request = new GetRequest.Builder()
                    .index("citydata")
                    .id(esDocId)
                    .build();

            GetResponse<JsonData> response = elasticsearchClient.get(request, JsonData.class);

            if (!response.found()) {
                throw new RuntimeException("해당 문서를 찾을 수 없습니다: " + esDocId);
            }

            // 2. JSON 파싱
            JsonNode root = objectMapper.readTree(response.source().toJson().toString());

            JsonNode cityData = root.path("CITYDATA");
            JsonNode ppltn = cityData.path("LIVE_PPLTN_STTS").path("LIVE_PPLTN_STTS");
            JsonNode weather = cityData.path("WEATHER_STTS").path("WEATHER_STTS");

            // 3. 필요한 필드만 DTO에 담기
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
            throw new RuntimeException("Elasticsearch 조회 또는 파싱 실패: " + e.getMessage(), e);
        }
    }
}
