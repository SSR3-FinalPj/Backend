package org.example.ssj3pj.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.EnvironmentMetadata;
import org.example.ssj3pj.repository.EnvironmentMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.example.ssj3pj.util.JsonNodeUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentQueryService {

    private static final String INDEX = "citydata";

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;
    private final EnvironmentMetadataRepository metadataRepository;

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

    @Transactional
    public void save(EnvironmentMetadata metadata) {
        metadataRepository.save(metadata);
    }

    @Transactional
    public void saveAll(List<EnvironmentMetadata> metadataList) {
        metadataRepository.saveAll(metadataList);
    }

    public EnvironmentSummaryDto getRecentSummaryByLocation(String locationCode) {
        try {
            ZoneId kstZoneId = ZoneId.of("Asia/Seoul");
            ZonedDateTime nowKst = ZonedDateTime.now(kstZoneId);
            ZonedDateTime oneHourAgoKst = nowKst.minusHours(1);

            String nowFormatted = nowKst.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")) + "Z";
            String oneHourAgoFormatted = oneHourAgoKst.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")) + "Z";

            log.info("Querying for location '{}' in time range: gte={}, lte={}", locationCode, oneHourAgoFormatted, nowFormatted);

            SearchRequest request = new SearchRequest.Builder()
                    .index(INDEX)
                    .query(q -> q.bool(b -> b
                            .filter(f -> f.term(t -> t.field("citydata.AREA_CD.keyword").value(locationCode)))
                            .filter(f -> f.range(r -> r.field("INDEXED_AT")
                                    .gte(JsonData.of(oneHourAgoFormatted))
                                    .lte(JsonData.of(nowFormatted))
                            ))
                    ))
                    .build();

            SearchResponse<JsonData> response = elasticsearchClient.search(request, JsonData.class);

            List<EnvironmentSummaryDto> results = new ArrayList<>();
            for (Hit<JsonData> hit : response.hits().hits()) {
                JsonNode root = objectMapper.readTree(hit.source().toJson().toString());
                JsonNode city = coalesce(root.path("citydata"), root.path("CITYDATA"));
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

                results.add(EnvironmentSummaryDto.builder()
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
                        .build());
            }

            if (results.isEmpty()) {
                return null;
            }

            double avgTemp = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getTemperature())).average().orElse(0.0);
            double avgHumidity = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getHumidity())).average().orElse(0.0);
            double avgUvIndex = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getUvIndex())).average().orElse(0.0);
            double avgMaleRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getMaleRate())).average().orElse(0.0);
            double avgFemaleRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getFemaleRate())).average().orElse(0.0);
            double avgTeenRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getTeenRate())).average().orElse(0.0);
            double avgTwentyRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getTwentyRate())).average().orElse(0.0);
            double avgThirtyRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getThirtyRate())).average().orElse(0.0);
            double avgFortyRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getFortyRate())).average().orElse(0.0);
            double avgFiftyRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getFiftyRate())).average().orElse(0.0);
            double avgSixtyRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getSixtyRate())).average().orElse(0.0);
            double avgSeventyRate = results.stream().mapToDouble(dto -> Double.parseDouble(dto.getSeventyRate())).average().orElse(0.0);

            return EnvironmentSummaryDto.builder()
                    .areaName(results.get(0).getAreaName())
                    .congestionLevel(results.get(0).getCongestionLevel())
                    .temperature(String.format("%.1f", avgTemp))
                    .humidity(String.format("%.1f", avgHumidity))
                    .uvIndex(String.format("%.1f", avgUvIndex))
                    .maleRate(String.format("%.1f", avgMaleRate))
                    .femaleRate(String.format("%.1f", avgFemaleRate))
                    .teenRate(String.format("%.1f", avgTeenRate))
                    .twentyRate(String.format("%.1f", avgTwentyRate))
                    .thirtyRate(String.format("%.1f", avgThirtyRate))
                    .fortyRate(String.format("%.1f", avgFortyRate))
                    .fiftyRate(String.format("%.1f", avgFiftyRate))
                    .sixtyRate(String.format("%.1f", avgSixtyRate))
                    .seventyRate(String.format("%.1f", avgSeventyRate))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("❌ ES 조회 또는 파싱 실패: " + e.getMessage(), e);
        }
    }
}