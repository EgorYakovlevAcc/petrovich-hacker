package ru.tomato.petrovichhacker.config;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.tomato.petrovichhacker.bot.PetrovichHackerBot;

public class BotInitializer {
    private PetrovichHackerBot petrovichHackerBot;
    private TelegramBotsApi telegramBotsApi;

    public BotInitializer(PetrovichHackerBot petrovichHackerBot, TelegramBotsApi telegramBotsApi) {
        try {
            telegramBotsApi.registerBot(petrovichHackerBot);
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
    }
}
