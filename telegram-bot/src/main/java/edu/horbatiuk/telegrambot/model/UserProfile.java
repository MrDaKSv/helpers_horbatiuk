package edu.horbatiuk.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "username")
    private String username;

    @Column(name = "language", nullable = false)
    @Builder.Default
    private String language = "uk";

    // англійською
    @Column(name = "favorite_city")
    private String favoriteCity;

    @Column(name = "base_currency", nullable = false)
    @Builder.Default
    private String baseCurrency = "USD";

    @Column(name = "request_count", nullable = false)
    @Builder.Default
    private int requestCount = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private long createdAt = System.currentTimeMillis();

    @Column(name = "last_seen")
    private long lastSeen;

    public void incrementRequests() {
        this.requestCount++;
        this.lastSeen = System.currentTimeMillis();
    }
}