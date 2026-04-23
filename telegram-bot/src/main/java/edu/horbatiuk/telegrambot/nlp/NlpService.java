package edu.horbatiuk.telegrambot.nlp;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NlpService {

    public enum Intent {
        WEATHER, CURRENCY, CONVERT, SET_CITY, SET_LANG,
        GET_PROFILE, GREETING, HELP, UNKNOWN
    }

    private static final Map<Intent, List<String>> INTENT_KEYWORDS = new LinkedHashMap<>();

    static {
        INTENT_KEYWORDS.put(Intent.WEATHER, Arrays.asList(
                "погода", "weather", "температура", "temperature",
                "дощ", "сніг", "хмарно", "сонячно", "спека", "мороз",
                "rain", "snow", "sunny", "forecast", "прогноз"
        ));
        INTENT_KEYWORDS.put(Intent.CONVERT, Arrays.asList(
                "конвертуй", "convert", "переведи", "скільки", "переведіть",
                "порахуй", "calculate", "exchange", "перерахуй"
        ));
        INTENT_KEYWORDS.put(Intent.CURRENCY, Arrays.asList(
                "курс", "валюта", "currency", "rate", "котирування",
                "долар", "євро", "гривня", "фунт", "злотий"
        ));
        INTENT_KEYWORDS.put(Intent.SET_CITY, Arrays.asList(
                "моє місто", "my city", "встанови місто", "set city",
                "змінити місто", "місто за замовчуванням"
        ));
        INTENT_KEYWORDS.put(Intent.GET_PROFILE, Arrays.asList(
                "профіль", "profile", "мої налаштування", "settings",
                "my settings", "налаштування"
        ));
        INTENT_KEYWORDS.put(Intent.GREETING, Arrays.asList(
                "привіт", "hello", "hi", "вітаю", "добрий день",
                "добридень", "добрий ранок", "доброго вечора", "хай"
        ));
        INTENT_KEYWORDS.put(Intent.HELP, Arrays.asList(
                "допоможи", "help", "що вмієш", "команди",
                "як користуватись", "інструкція"
        ));
    }

    private static final Map<String, String> CITY_FORMS = new LinkedHashMap<>();

    static {
        for (String f : new String[]{"київ","києві","київа","киеве","киев"})
            CITY_FORMS.put(f, "Kyiv");
        for (String f : new String[]{"харків","харкові","харкова","харьков","харькове"})
            CITY_FORMS.put(f, "Kharkiv");
        for (String f : new String[]{"одеса","одесі","одесу","одессе","одесса"})
            CITY_FORMS.put(f, "Odessa");
        for (String f : new String[]{"дніпро","дніпрі","дніпра","днепр","днепре"})
            CITY_FORMS.put(f, "Dnipro");
        for (String f : new String[]{"львів","львові","львова","льваве","львов"})
            CITY_FORMS.put(f, "Lviv");
        for (String f : new String[]{"чернівці","чернівцях","чернівців","черновцы","черновцах"})
            CITY_FORMS.put(f, "Chernivtsi");
        for (String f : new String[]{"вінниця","вінниці","вінницю","винница","виннице"})
            CITY_FORMS.put(f, "Vinnytsia");
        for (String f : new String[]{"запоріжжя","запоріжжі","запорожье"})
            CITY_FORMS.put(f, "Zaporizhzhia");
        for (String f : new String[]{"полтава","полтаві","полтаву","полтаве"})
            CITY_FORMS.put(f, "Poltava");
        for (String f : new String[]{"суми","сумах","сумів"})
            CITY_FORMS.put(f, "Sumy");
        for (String f : new String[]{"житомир","житомирі","житомира"})
            CITY_FORMS.put(f, "Zhytomyr");
        for (String f : new String[]{"тернопіль","тернополі","тернополя","тернополь"})
            CITY_FORMS.put(f, "Ternopil");
        for (String f : new String[]{"херсон","херсоні","херсона"})
            CITY_FORMS.put(f, "Kherson");
        for (String f : new String[]{"миколаїв","миколаєві","миколаєва","николаев"})
            CITY_FORMS.put(f, "Mykolaiv");
        for (String f : new String[]{"луцьк","луцьку","луцька","луцке"})
            CITY_FORMS.put(f, "Lutsk");
        for (String f : new String[]{"рівне","рівному","рівного","ровно"})
            CITY_FORMS.put(f, "Rivne");
        for (String f : new String[]{"ужгород","ужгороді","ужгорода","ужгороде"})
            CITY_FORMS.put(f, "Uzhhorod");
        for (String f : new String[]{"чернігів","чернігові","чернігова","чернигов"})
            CITY_FORMS.put(f, "Chernihiv");
        for (String f : new String[]{"хмельницький","хмельницькому","хмельницького"})
            CITY_FORMS.put(f, "Khmelnytskyi");
    }

    private static final Map<String, String> CURRENCY_NAMES = new HashMap<>();

    static {
        // USD — всі форми
        for (String f : new String[]{
                "долар","долара","доларів","доларах","долари","доларами",
                "доллар","доллара","долларов","долларах","доллары","долларами",
                "dollar","dollars","usd"})
            CURRENCY_NAMES.put(f, "USD");

        // EUR — всі форми
        for (String f : new String[]{
                "євро","євра","євром","euro","euros","eur"})
            CURRENCY_NAMES.put(f, "EUR");

        // UAH — всі форми
        for (String f : new String[]{
                "гривня","гривні","гривень","гривнях","гривнями","гривню",
                "гривна","гривен","гривнах","hryvnia","hryvna","uah"})
            CURRENCY_NAMES.put(f, "UAH");

        // GBP — всі форми
        for (String f : new String[]{
                "фунт","фунта","фунтів","фунтах","фунти","фунтами",
                "pound","pounds","gbp"})
            CURRENCY_NAMES.put(f, "GBP");

        // PLN — всі форми
        for (String f : new String[]{
                "злотий","злотого","злотих","злотих","zloty","pln"})
            CURRENCY_NAMES.put(f, "PLN");

        // CHF
        for (String f : new String[]{"франк","франка","франків","franc","francs","chf"})
            CURRENCY_NAMES.put(f, "CHF");

        // JPY
        for (String f : new String[]{"єна","єни","єн","yen","jpy"})
            CURRENCY_NAMES.put(f, "JPY");

        // CZK
        for (String f : new String[]{"крона","крони","крон","czk"})
            CURRENCY_NAMES.put(f, "CZK");

        // HUF
        for (String f : new String[]{"форинт","форинта","форинтів","huf"})
            CURRENCY_NAMES.put(f, "HUF");

        // CAD
        for (String f : new String[]{"канадський долар","cad"})
            CURRENCY_NAMES.put(f, "CAD");
    }

    private static final Pattern CONVERT_NUMBER_PATTERN = Pattern.compile(
            "([\\d]+(?:[.,]\\d+)?)"
    );

    @PostConstruct
    public void init() {
        log.info("NLP сервіс ініціалізовано. Міст: {}, валют: {}",
                CITY_FORMS.size(), CURRENCY_NAMES.size());
    }

    public NlpResult analyze(String text) {
        if (text == null || text.isBlank()) return NlpResult.unknown();

        String normalized = text.toLowerCase().trim();
        String[] tokens = tokenize(normalized);

        log.debug("NLP токени: {}", Arrays.toString(tokens));

        // 1. Спробуємо витягти конвертацію з будь-якого формату
        NlpResult convertResult = tryExtractConversion(tokens, normalized);
        if (convertResult != null) return convertResult;

        // 2. Визначаємо намір
        Intent intent = detectIntent(tokens, normalized);

        // 3. Витягуємо сутності
        String city     = extractCity(tokens, normalized);
        String currency = extractCurrency(tokens);

        return NlpResult.builder()
                .intent(intent)
                .city(city)
                .currency(currency)
                .rawText(text)
                .build();
    }

    private String[] tokenize(String text) {
        return text.split("[\\s,!?.;:\"'()\\[\\]{}]+");
    }

    private Intent detectIntent(String[] tokens, String fullText) {
        Map<Intent, Integer> scores = new EnumMap<>(Intent.class);
        Set<String> tokenSet = new HashSet<>(Arrays.asList(tokens));

        for (Map.Entry<Intent, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (tokenSet.contains(keyword))       score += 2;
                else if (fullText.contains(keyword))  score += 1;
            }
            if (score > 0) scores.put(entry.getKey(), score);
        }

        if (scores.isEmpty()) return Intent.UNKNOWN;

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Intent.UNKNOWN);
    }

    public String extractCity(String[] tokens, String fullText) {
        for (String token : tokens) {
            String city = CITY_FORMS.get(token.toLowerCase());
            if (city != null) return city;
        }
        for (Map.Entry<String, String> entry : CITY_FORMS.entrySet()) {
            if (fullText.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    public String extractCurrency(String[] tokens) {
        for (String token : tokens) {
            String code = CURRENCY_NAMES.get(token.toLowerCase());
            if (code != null) return code;
        }
        return null;
    }

    private NlpResult tryExtractConversion(String[] tokens, String fullText) {
        // Крок 1: знаходимо число
        double amount = -1;
        for (String token : tokens) {
            try {
                amount = Double.parseDouble(token.replace(',', '.'));
                break;
            } catch (NumberFormatException ignored) {}
        }
        if (amount < 0) return null;

        // Крок 2: знаходимо всі валюти в порядку появи в тексті
        List<String> foundCurrencies = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();

        for (Map.Entry<String, String> entry : CURRENCY_NAMES.entrySet()) {
            int pos = fullText.indexOf(entry.getKey());
            if (pos >= 0 && !foundCurrencies.contains(entry.getValue())) {
                foundCurrencies.add(entry.getValue());
                positions.add(pos);
            }
        }

        if (foundCurrencies.size() < 2) return null;

        // Крок 3: сортуємо за позицією — перша валюта "звідки", друга "куди"
        if (positions.get(0) > positions.get(1)) {
            Collections.swap(foundCurrencies, 0, 1);
        }

        String from = foundCurrencies.get(0);
        String to   = foundCurrencies.get(1);

        log.debug("Конвертація: {} {} -> {}", amount, from, to);

        return NlpResult.builder()
                .intent(Intent.CONVERT)
                .currency(from)
                .targetCurrency(to)
                .amount(amount)
                .rawText(fullText)
                .build();
    }

    public String normalizeCity(String cityInput) {
        if (cityInput == null) return null;
        return CITY_FORMS.getOrDefault(cityInput.toLowerCase().trim(), cityInput);
    }
}