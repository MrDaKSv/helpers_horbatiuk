package edu.horbatiuk.apiservice.service;

import edu.horbatiuk.apiservice.model.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestTemplate restTemplate;

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.url}")
    private String apiUrl;

    @Value("${openweather.api.units:metric}")
    private String units;

    @Value("${openweather.api.lang:ua}")
    private String lang;

    @Cacheable(value = "weather", key = "#city.toLowerCase()")
    public WeatherResponse getWeather(String city) {
        log.info("Fetching weather for city: {}", city);
        log.info("OPENWEATHER KEY = {}", apiKey);

        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("q", city)
                .queryParam("appid", apiKey)
                .queryParam("units", units)
                .queryParam("lang", lang)
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return parseWeatherResponse(response);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("City not found: {}", city);
            throw new RuntimeException("Місто не знайдено: " + city);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Invalid OpenWeather API key");
            throw new RuntimeException("Невірний API ключ для OpenWeather");
        } catch (ResourceAccessException e) {
            log.error("OpenWeather API is unavailable: {}", e.getMessage());
            throw new RuntimeException("Сервіс погоди тимчасово недоступний");
        } catch (Exception e) {
            log.error("Error fetching weather for {}: {}", city, e.getMessage());
            throw new RuntimeException("Помилка отримання даних про погоду: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private WeatherResponse parseWeatherResponse(Map<String, Object> data) {
        Map<String, Object> main = (Map<String, Object>) data.get("main");
        Map<String, Object> wind = (Map<String, Object>) data.get("wind");
        Map<String, Object> sys = (Map<String, Object>) data.get("sys");
        java.util.List<Map<String, Object>> weather =
                (java.util.List<Map<String, Object>>) data.get("weather");

        return WeatherResponse.builder()
                .city((String) data.get("name"))
                .country((String) sys.get("country"))
                .temperature(toDouble(main.get("temp")))
                .feelsLike(toDouble(main.get("feels_like")))
                .humidity((Integer) main.get("humidity"))
                .description((String) weather.get(0).get("description"))
                .windSpeed(toDouble(wind.get("speed")))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private double toDouble(Object value) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}
