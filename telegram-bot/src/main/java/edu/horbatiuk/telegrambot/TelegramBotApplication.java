package edu.horbatiuk.telegrambot;

import edu.horbatiuk.telegrambot.handler.InfoBot;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling  // ← ЛАБ 5: вмикає @Scheduled в ReminderService
public class TelegramBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelegramBotApplication.class, args);
	}

	@Bean
	public ApplicationRunner botRunner(InfoBot infoBot) {
		return args -> {
			try {
				TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
				botsApi.registerBot(infoBot);
				System.out.println("✅ Бот зареєстрований: " + infoBot.getBotUsername());
			} catch (TelegramApiException e) {
				System.err.println("❌ Помилка реєстрації: " + e.getMessage());
				throw new RuntimeException(e);
			}
		};
	}
}