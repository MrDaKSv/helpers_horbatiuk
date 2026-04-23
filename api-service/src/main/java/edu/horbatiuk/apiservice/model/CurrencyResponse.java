package edu.horbatiuk.apiservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyResponse {

    @JsonProperty("base_currency")
    private String baseCurrency;

    private Map<String, Double> rates;

    @JsonProperty("cached")
    private boolean cached;

    @JsonProperty("timestamp")
    private long timestamp;
}