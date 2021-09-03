package ru.tomato.petrovichhacker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.tomato.petrovichhacker.service.GoodsAvailabilityChecker;
import ru.tomato.petrovichhacker.service.impl.GoodsAvailabilityCheckerImpl;
import ru.tomato.petrovichhacker.service.PetrovichService;
import ru.tomato.petrovichhacker.service.impl.PetrovichServiceImpl;
import ru.tomato.petrovichhacker.bot.PetrovichHackerBot;

@Configuration
public class PetrovichHackerConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public PetrovichService petrovichService(RestTemplate restTemplate, PetrovichHackerBot bot) {
        return new PetrovichServiceImpl(bot, restTemplate);
    }

    @Bean
    public PetrovichHackerBot petrovichHackerBot() {
        return new PetrovichHackerBot();
    }

    @Bean
    public BotInitializer botInitializer(PetrovichHackerBot petrovichHackerBot, TelegramBotsApi telegramBotsApi) {
        return new BotInitializer(petrovichHackerBot, telegramBotsApi);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(){
        try {
            return new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Bean
    public GoodsAvailabilityChecker goodsAvailabilityChecker(PetrovichService petrovichService) {
        return new GoodsAvailabilityCheckerImpl(petrovichService);
    }
}
