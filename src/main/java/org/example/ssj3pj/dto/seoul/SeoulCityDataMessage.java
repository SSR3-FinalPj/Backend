package org.example.ssj3pj.dto.seoul;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SeoulCityDataMessage(
    @JsonProperty("PRI_KEY") String priKey,
    @JsonProperty("list_total_count") String listTotalCount,
    @JsonProperty("RESULT") Result result,
    @JsonProperty("CITYDATA") CityData cityData
) {
    public record Result(
        @JsonProperty("RESULT.CODE") String code,
        @JsonProperty("RESULT.MESSAGE") String message
    ) {}

    public record CityData(
        @JsonProperty("AREA_NM") String areaNm,
        @JsonProperty("AREA_CD") String areaCd,
        @JsonProperty("WEATHER_STTS") WeatherStts weatherStts
    ) {}

    public record WeatherStts(
        @JsonProperty("WEATHER_STTS") WeatherDetail weatherStts
    ) {}

    public record WeatherDetail(
        @JsonProperty("WEATHER_TIME") String weatherTime,
        @JsonProperty("TEMP") String temp,
        @JsonProperty("HUMIDITY") String humidity
    ) {}
}
