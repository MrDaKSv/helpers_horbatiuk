package edu.horbatiuk.apiservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeatherResponse {

    private String city;
    private String country;

    @JsonProperty("temperature")
    private double temperature;

    @JsonProperty("feels_like")
    private double feelsLike;

    @JsonProperty("humidity")
    private int humidity;

    private String description;

    @JsonProperty("wind_speed")
    private double windSpeed;

    @JsonProperty("cached")
    private boolean cached;

    @JsonProperty("timestamp")
    private long timestamp;
}