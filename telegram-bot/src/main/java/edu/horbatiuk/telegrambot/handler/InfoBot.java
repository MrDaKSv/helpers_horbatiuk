package edu.horbatiuk.telegrambot.handler;

import edu.horbatiuk.telegrambot.model.UserProfile;
import edu.horbatiuk.telegrambot.nlp.NlpResult;
import edu.horbatiuk.telegrambot.nlp.NlpService;
import edu.horbatiuk.telegrambot.service.*;
import edu.horbatiuk.telegrambot.service.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * Головний обробник Telegram бота.
 *
 * ЛАБ 1-2: Погода, курси валют, конвертація
 * ЛАБ 3:   NLP — розпізнавання вільного тексту
 * ЛАБ 4:   Профіль користувача (SQLite)
 * ЛАБ 5:   Нагадування з планувальником (@Scheduled)
 */
@Slf4j
@Component
@EnableScheduling
public class InfoBot extends TelegramLongPollingBot {

    private final ApiClientService api;
    private final NlpService nlp;
    private final UserProfileService profiles;
    private final ReminderService reminders;     // ЛАБ 5


    @Value("${telegram.bot.username}")
    private String botUsername;

    public InfoBot(ApiClientService api, NlpService nlp, UserProfileService profiles,
                   ReminderService reminders,
                   @Value("${telegram.bot.token}") String token) {
        super(token);
        this.api = api;
        this.nlp = nlp;
        this.profiles = profiles;
        this.reminders = reminders;
    }

    @PostConstruct
    public void init() {
        reminders.setMessageSender(this::send);
        log.info("InfoBot ініціалізовано з усіма сервісами (лаб 1-5)");
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        var telegramUser = update.getMessage().getFrom();

        log.info("Повідомлення від {} (chatId={}): {}", telegramUser.getFirstName(), chatId, text);

        UserProfile profile = profiles.recordRequest(chatId, telegramUser);

        try {


            String reply = process(text, profile);
            send(chatId, reply);

        } catch (Exception e) {
            log.error("Помилка обробки запиту: {}", e.getMessage(), e);
            send(chatId, "❌ Внутрішня помилка. Спробуйте ще раз.");
        }
    }

    // ── Маршрутизація ─────────────────────────────────────────────────────

    private String process(String text, UserProfile profile) {
        if (text.startsWith("/")) {
            return handleCommand(text, profile);
        }

        return handleNlp(text, profile);
    }

    // ── Обробка команд ────────────────────────────────────────────────────

    private String handleCommand(String text, UserProfile profile) {
        String cmd = text.split("\\s+")[0].toLowerCase();
        String arg = text.contains(" ") ? text.substring(text.indexOf(' ') + 1).trim() : "";

        return switch (cmd) {

            // ── Базові (ЛАБ 1-2) ──────────────────────────────────────────
            case "/start"    -> startMessage(profile);
            case "/help"     -> helpMessage();

            case "/weather"  -> {
                String city = arg.isEmpty() ? profile.getFavoriteCity() : arg;
                if (city == null || city.isBlank())
                    yield "🏙 Вкажіть місто або збережіть: `/setcity Kyiv`";
                yield api.getWeather(city);
            }

            case "/currency" -> {
                String base = arg.isEmpty() ? profile.getBaseCurrency() : arg.toUpperCase();
                yield api.getRates(base);
            }

            case "/convert"  -> handleConvertCommand(arg);

            // ── Профіль (ЛАБ 4) ───────────────────────────────────────────
            case "/profile"  -> profiles.formatProfile(profile);

            case "/setcity"  -> {
                if (arg.isBlank()) yield "❓ `/setcity Kyiv`";
                String norm = nlp.normalizeCity(arg);
                profiles.setFavoriteCity(profile.getChatId(), norm);
                yield "✅ Місто: *" + norm + "*";
            }

            case "/setcurrency" -> {
                if (arg.isBlank() || arg.length() != 3)
                    yield "❓ `/setcurrency EUR`";
                profiles.setBaseCurrency(profile.getChatId(), arg);
                yield "✅ Валюта: *" + arg.toUpperCase() + "*";
            }

            case "/setlang" -> {
                if (!arg.equals("uk") && !arg.equals("en"))
                    yield "❓ Доступні: `uk` або `en`";
                profiles.setLanguage(profile.getChatId(), arg);
                yield "uk".equals(arg) ? "✅ Мова: 🇺🇦 Українська" : "✅ Language: 🇬🇧 English";
            }

            // ── Нагадування (ЛАБ 5) ───────────────────────────────────────
            case "/remind"     -> reminders.createReminder(profile.getChatId(), arg);
            case "/reminders"  -> reminders.listReminders(profile.getChatId());
            case "/delremind"  -> reminders.deleteReminder(profile.getChatId(), arg);

            default -> "❓ Невідома команда. /help";
        };
    }


    // ── NLP (ЛАБ 3) ───────────────────────────────────────────────────────

    private String handleNlp(String text, UserProfile profile) {
        NlpResult result = nlp.analyze(text);

        log.info("NLP: intent={}, city={}, currency={}, amount={}",
                result.getIntent(), result.getCity(),
                result.getCurrency(), result.getAmount());

        return switch (result.getIntent()) {
            case WEATHER -> {
                String city = result.hasCity() ? result.getCity() : profile.getFavoriteCity();
                if (city == null)
                    yield "🏙 Не зрозумів місто. Спробуйте: *погода в Чернівцях*";
                yield api.getWeather(city);
            }
            case CURRENCY -> {
                String base = result.hasCurrency() ? result.getCurrency() : profile.getBaseCurrency();
                yield api.getRates(base);
            }
            case CONVERT -> {
                if (!result.hasCurrency() || !result.hasTargetCurrency())
                    yield "❓ Не зрозумів. Спробуйте: *100 USD в UAH*";
                yield api.convert(result.getCurrency(), result.getTargetCurrency(),
                        result.hasAmount() ? result.getAmount() : 1.0);
            }
            case GET_PROFILE -> profiles.formatProfile(profile);
            case GREETING    -> "👋 Привіт, *" + profile.getFirstName() + "*! /help";
            case HELP        -> helpMessage();
            default -> "🤔 Не зрозумів.\n\nСпробуйте:\n" +
                    "• _погода в Харкові_\n• _курс долара_\n" +
                    "• _Скільки 100 доларів у гривнях_\n• /help";
        };
    }

    // ── Конвертація через команду ─────────────────────────────────────────

    private String handleConvertCommand(String args) {
        NlpResult result = nlp.analyze(args);
        if (result.getIntent() == NlpService.Intent.CONVERT
                && result.hasCurrency() && result.hasTargetCurrency()) {
            return api.convert(result.getCurrency(), result.getTargetCurrency(),
                    result.hasAmount() ? result.getAmount() : 1.0);
        }
        return "❓ Формат: `/convert 100 USD to UAH`";
    }

    // ── Повідомлення ──────────────────────────────────────────────────────

    private String startMessage(UserProfile p) {
        return String.format(
                "👋 *Привіт, %s!*\n\n" +
                        "Я твій помічник:\n\n" +
                        "🌤 Погода\n" +
                        "💰 Курси валют\n" +
                        "💱 Конвертація\n" +
                        "🔔 Нагадування\n\n" +
                        "👇 Обери дію кнопками або напиши запит",
                p.getFirstName()
        );
    }

    private String helpMessage() {
        return "📖 *Допомога*\n\n" +
                "🌤 `/weather`\n" +
                "💰 `/currency USD`\n" +
                "💱 `/convert`\n\n" +
                "👤 `/profile`\n" +
                "🔔 `/remind завтра 10:00 Пари`\n\n" +
                "💡 Або просто напиши:\n" +
                "_погода в Чернівцях_";
    }

    // ── Відправка тексту ──────────────────────────────────────────────────

    private void send(long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Помилка відправки: {}", e.getMessage());
        }
    }
}