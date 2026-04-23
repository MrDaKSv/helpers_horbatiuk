package edu.horbatiuk.apiservice.controller;

import edu.horbatiuk.apiservice.model.WeatherResponse;
import edu.horbatiuk.apiservice.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * GET /weather?city=Kyiv
     * Returns current weather for the specified city
     */
    @GetMapping
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam(defaultValue = "Kyiv") String city) {

        log.info("GET /weather - city: {}", city);
        WeatherResponse response = weatherService.getWeather(city);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /weather/{city}
     * Returns current weather for the specified city (path variable)
     */
    @GetMapping("/{city}")
    public ResponseEntity<WeatherResponse> getWeatherByPath(@PathVariable String city) {
        log.info("GET /weather/{} ", city);
        WeatherResponse response = weatherService.getWeather(city);
        return ResponseEntity.ok(response);
    }
}