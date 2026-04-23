package edu.horbatiuk.telegrambot.service;

import edu.horbatiuk.telegrambot.model.UserProfile;
import edu.horbatiuk.telegrambot.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    public UserProfile getOrCreate(Long chatId, User telegramUser) {
        return repository.findById(chatId).orElseGet(() -> {
            log.info("Створення нового профілю для chatId={}", chatId);
            UserProfile profile = UserProfile.builder()
                    .chatId(chatId)
                    .firstName(telegramUser.getFirstName())
                    .username(telegramUser.getUserName())
                    .build();
            return repository.save(profile);
        });
    }


    public UserProfile recordRequest(Long chatId, User telegramUser) {
        UserProfile profile = getOrCreate(chatId, telegramUser);
        profile.incrementRequests();
        return repository.save(profile);
    }


    public UserProfile setFavoriteCity(Long chatId, String city) {
        UserProfile profile = repository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Профіль не знайдено"));
        profile.setFavoriteCity(city);
        return repository.save(profile);
    }


    public UserProfile setBaseCurrency(Long chatId, String currency) {
        UserProfile profile = repository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Профіль не знайдено"));
        profile.setBaseCurrency(currency.toUpperCase());
        return repository.save(profile);
    }


    public UserProfile setLanguage(Long chatId, String language) {
        UserProfile profile = repository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Профіль не знайдено"));
        profile.setLanguage(language);
        return repository.save(profile);
    }


    public String formatProfile(UserProfile p) {
        String city = p.getFavoriteCity() != null ? p.getFavoriteCity() : "не встановлено";
        String lastSeen = p.getLastSeen() > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getLastSeen()),
                ZoneId.systemDefault()).format(FORMATTER)
                : "—";
        String lang = "uk".equals(p.getLanguage()) ? "🇺🇦 Українська" : "🇬🇧 English";

        return String.format(
                "👤 *Ваш профіль*\n\n" +
                        "🏷 Ім'я: *%s*\n" +
                        "🌍 Мова: %s\n" +
                        "🏙 Улюблене місто: *%s*\n" +
                        "💰 Базова валюта: *%s*\n" +
                        "📊 Запитів всього: *%d*\n" +
                        "🕐 Остання активність: %s\n\n" +
                        "⚙️ *Команди налаштувань:*\n" +
                        "`/setcity Kyiv` — встановити місто\n" +
                        "`/setcurrency EUR` — базова валюта\n" +
                        "`/setlang uk` або `/setlang en` — мова",
                p.getFirstName(), lang, city, p.getBaseCurrency(),
                p.getRequestCount(), lastSeen
        );
    }
}