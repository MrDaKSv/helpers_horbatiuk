package edu.horbatiuk.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * ЛАБ 5 — Нагадування користувача.
 * Зберігається в SQLite таблиці reminders.
 */
@Entity
@Table(name = "reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Telegram chat ID власника нагадування */
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    /** Текст нагадування */
    @Column(name = "text", nullable = false)
    private String text;

    /** Час спрацювання (unix millis) */
    @Column(name = "remind_at", nullable = false)
    private long remindAt;

    /** Чи вже надіслано */
    @Column(name = "sent", nullable = false)
    @Builder.Default
    private boolean sent = false;

    /** Коли створено */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private long createdAt = System.currentTimeMillis();
}