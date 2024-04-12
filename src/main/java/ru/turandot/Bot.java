package ru.turandot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Bot extends TelegramLongPollingBot {
    private final String auth_token = "6783093196:AAF9vNB7gLtIwLyfxiPVhzE8-KZ_zkWdAZs";

    @Override
    public String getBotUsername() {
        return "Turandot Lingerie";
    }

    @Override
    public String getBotToken() {
        return auth_token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update);
    }
}
