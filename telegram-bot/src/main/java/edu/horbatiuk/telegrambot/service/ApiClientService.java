package edu.horbatiuk.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiClientService {

    private final RestTemplate restTemplate;

    @Value("${api.service.url:http://localhost:8080}")
    private String apiUrl;

    // Словник українських назв міст → англійські для OpenWeather API
    private static final Map<String, String> CITY_MAP = Map.ofEntries(
            Map.entry("київ",       "Kyiv"),
            Map.entry("харків",     "Kharkiv"),
            Map.entry("одеса",      "Odessa"),
            Map.entry("дніпро",     "Dnipro"),
            Map.entry("львів",      "Lviv"),
            Map.entry("запоріжжя",  "Zaporizhzhia"),
            Map.entry("миколаїв",   "Mykolaiv"),
            Map.entry("вінниця",    "Vinnytsia"),
            Map.entry("херсон",     "Kherson"),
            Map.entry("полтава",    "Poltava"),
            Map.entry("чернігів",   "Chernihiv"),
            Map.entry("черкаси",    "Cherkasy"),
            Map.entry("суми",       "Sumy"),
            Map.entry("житомир",    "Zhytomyr"),
            Map.entry("рівне",      "Rivne"),
            Map.entry("тернопіль",  "Ternopil"),
            Map.entry("луцьк",      "Lutsk"),
            Map.entry("ужгород",    "Uzhhorod"),
            Map.entry("чернівці",   "Chernivtsi"),
            Map.entry("хмельницький", "Khmelnytskyi"),
            Map.entry("івано-франківськ", "Ivano-Frankivsk")
    );

    public String getWeather(String city) {
        try {
            String translatedCity = CITY_MAP.getOrDefault(city.toLowerCase().trim(), city);
            log.info("Запит погоди: '{}' -> '{}'", city, translatedCity);

            Map<String, Object> data = restTemplate.getForObject(
                    apiUrl + "/weather?city=" + translatedCity, Map.class);
            return formatWeather(data);
        } catch (ResourceAccessException e) {
            return "❌ API сервіс недоступний. Переконайтесь що api-service запущений на порту 8080.";
        } catch (Exception e) {
            log.error("Weather error: {}", e.getMessage());
            return "❌ Місто не знайдено: *" + city + "*\n\nСпробуйте англійською: `/weather Chernivtsi`";
        }
    }

    public String getRates(String base) {
        try {
            Map<String, Object> data = restTemplate.getForObject(
                    apiUrl + "/currency?base=" + base, Map.class);
            return formatRates(data);
        } catch (ResourceAccessException e) {
            return "❌ API сервіс недоступний.";
        } catch (Exception e) {
            log.error("Currency error: {}", e.getMessage());
            return "❌ Помилка: " + extractMessage(e);
        }
    }

    public String convert(String from, String to, double amount) {
        try {
            String url = String.format(Locale.US, "%s/currency/convert?from=%s&to=%s&amount=%.2f",
                    apiUrl, from.toUpperCase(), to.toUpperCase(), amount);

            Map<String, Object> data = restTemplate.getForObject(url, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) data.get("rates");
            double result = toDouble(rates.get(to.toUpperCase()));

            return String.format(Locale.US, "💱 *Конвертація*\n\n%.2f %s = *%.2f %s*",
                    amount, from.toUpperCase(), result, to.toUpperCase());
        } catch (Exception e) {
            return "❌ Помилка конвертації: " + extractMessage(e);
        }
    }

    @SuppressWarnings("unchecked")
    private String formatWeather(Map<String, Object> d) {
        return String.format(Locale.US,
                "🌤 *Погода в %s*\n\n" +
                        "🌡 Температура: *%.1f°C*\n" +
                        "🤔 Відчувається: *%.1f°C*\n" +
                        "💧 Вологість: *%d%%*\n" +
                        "🌬 Вітер: *%.1f м/с*\n\n" +
                d.get("city"), d.get("country"),
                toDouble(d.get("temperature")),
                toDouble(d.get("feels_like")),
                ((Number) d.getOrDefault("humidity", 0)).intValue(),
                toDouble(d.get("wind_speed")),
                d.getOrDefault("description", ""));
    }

    @SuppressWarnings("unchecked")
    private String formatRates(Map<String, Object> d) {
        String base = (String) d.getOrDefault("base_currency", "USD");
        Map<String, Object> rates = (Map<String, Object>) d.get("rates");
        StringBuilder sb = new StringBuilder("💰 *Курси відносно " + base + "*\n\n");
        if (rates != null) {
            rates.entrySet().stream()
                    .filter(e -> !e.getKey().equals(base))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sb.append(
                            String.format(Locale.US, "%-5s -> %.4f\n", e.getKey(), toDouble(e.getValue()))));
        }
        return sb.toString();
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private String extractMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Невідома помилка";
    }
}