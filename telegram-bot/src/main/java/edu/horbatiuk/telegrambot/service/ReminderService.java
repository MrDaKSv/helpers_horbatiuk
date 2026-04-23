package edu.horbatiuk.telegrambot.service;

import edu.horbatiuk.telegrambot.model.Reminder;
import edu.horbatiuk.telegrambot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository repository;

    // Callback для відправки повідомлень через бота (встановлюється в InfoBot)
    private java.util.function.BiConsumer<Long, String> messageSender;

    public void setMessageSender(java.util.function.BiConsumer<Long, String> sender) {
        this.messageSender = sender;
    }

    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final DateTimeFormatter FULL_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Патерн: дата/час + текст
    // Підтримує: "25.03 15:30 текст", "завтра 9:00 текст", "через 2 години текст"
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile(
            "^(\\d{1,2}\\.\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(.+)$"
    );
    private static final Pattern TOMORROW_PATTERN = Pattern.compile(
            "^(?:завтра|tomorrow)\\s+(\\d{1,2}:\\d{2})\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RELATIVE_PATTERN = Pattern.compile(
            "^(?:через|in)\\s+(\\d+)\\s+(?:хвилин|хвилини|хвилину|хв|хвил|minutes?|min)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern RELATIVE_HOURS_PATTERN = Pattern.compile(
            "^(?:через|in)\\s+(\\d+)\\s+(?:годин|години|годину|год|hours?|h)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );


    public String createReminder(Long chatId, String args) {
        if (args.isBlank()) {
            return """
                    ❓ *Формат нагадувань:*
                    
                    `/remind 25.03 15:30 Зустріч`
                    `/remind завтра 9:00 Лекція`
                    `/remind через 30 хвилин Подзвонити`
                    `/remind через 2 години Відповісти на email`""";
        }

        try {
            LocalDateTime remindAt = null;
            String text = null;

            // Спробуємо різні формати
            Matcher m1 = DATE_TIME_PATTERN.matcher(args);
            Matcher m2 = TOMORROW_PATTERN.matcher(args);
            Matcher m3 = RELATIVE_PATTERN.matcher(args);
            Matcher m4 = RELATIVE_HOURS_PATTERN.matcher(args);

            if (m1.matches()) {
                // "25.03 15:30 текст"
                String[] dateParts = m1.group(1).split("\\.");
                String[] timeParts = m1.group(2).split(":");
                int year = LocalDateTime.now().getYear();
                remindAt = LocalDateTime.of(year,
                        Integer.parseInt(dateParts[1]),
                        Integer.parseInt(dateParts[0]),
                        Integer.parseInt(timeParts[0]),
                        Integer.parseInt(timeParts[1]));
                text = m1.group(3);

            } else if (m2.matches()) {
                // "завтра 9:00 текст"
                String[] timeParts = m2.group(1).split(":");
                remindAt = LocalDateTime.now()
                        .plusDays(1)
                        .withHour(Integer.parseInt(timeParts[0]))
                        .withMinute(Integer.parseInt(timeParts[1]))
                        .withSecond(0);
                text = m2.group(2);

            } else if (m3.matches()) {
                // "через 30 хвилин текст"
                remindAt = LocalDateTime.now().plusMinutes(Long.parseLong(m3.group(1)));
                text = m3.group(2);

            } else if (m4.matches()) {
                // "через 2 години текст"
                remindAt = LocalDateTime.now().plusHours(Long.parseLong(m4.group(1)));
                text = m4.group(2);

            } else {
                return "❌ Не вдалося розпізнати час. Спробуйте:\n`/remind завтра 10:00 Важлива зустріч`";
            }

            long remindAtMillis = remindAt
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            Reminder reminder = Reminder.builder()
                    .chatId(chatId)
                    .text(text)
                    .remindAt(remindAtMillis)
                    .build();
            repository.save(reminder);

            String formattedTime = remindAt.format(FULL_FORMAT);
            log.info("Нагадування створено: chatId={}, time={}, text={}", chatId, formattedTime, text);

            return String.format("✅ *Нагадування встановлено*\n\n📅 %s\n📝 %s", formattedTime, text);

        } catch (Exception e) {
            log.error("Помилка створення нагадування: {}", e.getMessage());
            return "❌ Помилка: " + e.getMessage();
        }
    }


    public String listReminders(Long chatId) {
        List<Reminder> reminders = repository
                .findByChatIdAndSentFalseOrderByRemindAtAsc(chatId);

        if (reminders.isEmpty()) {
            return "📭 У вас немає активних нагадувань.\n\nДодати: `/remind завтра 10:00 текст`";
        }

        StringBuilder sb = new StringBuilder("📋 *Ваші нагадування:*\n\n");
        for (Reminder r : reminders) {
            LocalDateTime dt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(r.getRemindAt()),
                    ZoneId.systemDefault());
            sb.append(String.format("🔔 *#%d* | %s\n📝 %s\n\n",
                    r.getId(), dt.format(FULL_FORMAT), r.getText()));
        }
        sb.append("Видалити: `/delremind <номер>`");
        return sb.toString();
    }


    public String deleteReminder(Long chatId, String idStr) {
        try {
            long id = Long.parseLong(idStr.trim());
            Reminder reminder = repository.findById(id)
                    .orElse(null);

            if (reminder == null || !reminder.getChatId().equals(chatId)) {
                return "❌ Нагадування #" + id + " не знайдено.";
            }
            repository.delete(reminder);
            return "✅ Нагадування #" + id + " видалено.";
        } catch (NumberFormatException e) {
            return "❌ Вкажіть номер нагадування: `/delremind 3`";
        }
    }


    @Scheduled(fixedDelay = 30000)
    public void checkReminders() {
        if (messageSender == null) return;

        long now = System.currentTimeMillis();
        List<Reminder> due = repository.findBySentFalseAndRemindAtLessThanEqual(now);

        for (Reminder r : due) {
            log.info("Відправка нагадування #{} для chatId={}", r.getId(), r.getChatId());
            messageSender.accept(r.getChatId(),
                    "🔔 *Нагадування!*\n\n" + r.getText());
            r.setSent(true);
            repository.save(r);
        }
    }
}