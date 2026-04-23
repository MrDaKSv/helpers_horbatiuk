package edu.horbatiuk.apiservice.service;

import edu.horbatiuk.apiservice.model.CurrencyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final RestTemplate restTemplate;

    @Value("${exchangerates.api.key}")
    private String apiKey;

    @Value("${exchangerates.api.url}")
    private String apiUrl;

    @Cacheable(value = "currency", key = "#baseCurrency.toUpperCase()")
    public CurrencyResponse getRates(String baseCurrency) {
        log.info("Fetching currency rates for base: {}", baseCurrency);

        String url = String.format("%s/%s/latest/%s", apiUrl, apiKey, baseCurrency.toUpperCase());

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (!"success".equals(response.get("result"))) {
                throw new RuntimeException("Не вдалося отримати курси валют");
            }

            // ← ВИПРАВЛЕНО: API повертає мішанину Integer і Double
            // тому беремо Map<String, Object> і конвертуємо через toDouble()
            @SuppressWarnings("unchecked")
            Map<String, Object> conversionRates =
                    (Map<String, Object>) response.get("conversion_rates");

            Map<String, Double> filteredRates = filterPopularCurrencies(conversionRates);

            return CurrencyResponse.builder()
                    .baseCurrency(baseCurrency.toUpperCase())
                    .rates(filteredRates)
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (ResourceAccessException e) {
            log.error("ExchangeRates API is unavailable: {}", e.getMessage());
            throw new RuntimeException("Сервіс обміну валют тимчасово недоступний");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching currency rates: {}", e.getMessage());
            throw new RuntimeException("Помилка отримання курсів валют: " + e.getMessage());
        }
    }

    public CurrencyResponse convertCurrency(String from, String to, double amount) {
        log.info("Converting {} {} to {}", amount, from, to);
        CurrencyResponse rates = getRates(from);

        Double toRate = rates.getRates().get(to.toUpperCase());
        if (toRate == null) {
            throw new RuntimeException("Валюта не підтримується: " + to);
        }

        double converted = amount * toRate;
        Map<String, Double> result = new HashMap<>();
        result.put(to.toUpperCase(), Math.round(converted * 100.0) / 100.0);
        result.put("amount_requested", amount);

        return CurrencyResponse.builder()
                .baseCurrency(from.toUpperCase())
                .rates(result)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ← ВИПРАВЛЕНО: приймає Map<String, Object> замість Map<String, Double>
    // і конвертує кожне значення через toDouble() щоб обробити і Integer і Double
    private Map<String, Double> filterPopularCurrencies(Map<String, Object> all) {
        String[] popular = {"USD", "EUR", "GBP", "PLN", "UAH", "CHF", "JPY", "CZK", "HUF", "CAD"};
        Map<String, Double> filtered = new HashMap<>();
        for (String code : popular) {
            if (all.containsKey(code)) {
                filtered.put(code, toDouble(all.get(code)));
            }
        }
        return filtered;
    }

    // ← ВИПРАВЛЕНО: універсальний метод який обробляє Integer, Double, Long і т.д.
    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}