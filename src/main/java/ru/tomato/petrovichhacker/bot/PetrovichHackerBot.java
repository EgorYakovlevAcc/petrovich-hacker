package ru.tomato.petrovichhacker.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.tomato.petrovichhacker.PetrovichHackerUtils;
import ru.tomato.petrovichhacker.pojo.GoodDetails;
import ru.tomato.petrovichhacker.pojo.WarehouseDeliveryDetails;

public class PetrovichHackerBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(PetrovichHackerBot.class);
    private static final String SUCCESS_NOTIFICATION = "✅ Товар [%s] в наличии!\n\n";
    private static final String STRANGE_NOTIFICATION = "❓ С товаром [%s] происходит что-то странное...\n\n%s";
    private static final String AVAILABLE_FOR_BOOKING_NOTIFICATION = "\uD83D\uDE9A Товар [%s] доступен только ПОД ЗАКАЗ.\n\n%s";
    @Value("${petrovichhacker.chat.id}")
    private String chatId;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Chat chat = update.getMessage().getChat();
        LOGGER.debug("Get update from user {}", chat.getUserName());
        LOGGER.info("Update is sent from chat: {}", chat.getId());
    }

    @Override
    public String getBotUsername() {
        return "petrovichHackerBot";
    }

    @Override
    public String getBotToken() {
        return "1916902643:AAGn9NFIzdYTdpp8Q49Fm9uiPtzbpDsQc0g";
    }

    public void notifyAboutStranges(String goodId) {
        String goodUrl = PetrovichHackerUtils.getUrl(goodId);
        LOGGER.warn("Bot notifies about stranges for good [{}].", goodId);
        sendMsg(String.format(STRANGE_NOTIFICATION, goodId, goodUrl));
    }

    public void notifyAboutGoodsAvailableForBooking(String goodId) {
        String goodUrl = PetrovichHackerUtils.getUrl(goodId);
        LOGGER.info("Good [{}] is just available for booking.", goodId);
        sendMsg(String.format(AVAILABLE_FOR_BOOKING_NOTIFICATION, goodId, goodUrl));
    }

    public void notifyAboutSuccess(String goodId, GoodDetails goodDetails) {
        String goodUrl = PetrovichHackerUtils.getUrl(goodId);
        LOGGER.info("Bot notifies about success with good [{}].", goodId);
        StringBuilder sb = new StringBuilder(String.format(SUCCESS_NOTIFICATION, goodId));
        if (goodDetails != null) {
            sb.append("Возможные варианты получения:\n\n");
            sb.append("Доставим:\n");
            sb.append(goodDetails.getDeliveryTime());
            sb.append(": ");
            sb.append(goodDetails.getAvailableForDeliveryAmount());
            sb.append("\n\n");

            sb.append("Забрать со склада:");
            for (WarehouseDeliveryDetails warehouseDeliveryDetails : goodDetails.getWarehouseDeliveryDetailsList()) {
                sb.append("\n");
                sb.append(warehouseDeliveryDetails.getName());
                sb.append(": ");
                sb.append(warehouseDeliveryDetails.getAmount());
            }
        }

        sb.append("\n\n" + goodUrl);
        sendMsg(sb.toString());
    }

    private void sendMsg(String msg) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(msg);
        sendMessage.setChatId(this.getChatId());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
