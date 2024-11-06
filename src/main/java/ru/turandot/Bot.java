package ru.turandot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

class RandomStringGenerator {

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static final Random random = new SecureRandom();

    public static String generateRandomString(int length) {
        if (length < 1) throw new IllegalArgumentException();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);

            sb.append(rndChar);
        }

        return sb.toString();
    }
}

class PromocodesDB {
    private Map<String, Boolean> promocodes;

    public PromocodesDB() {
        promocodes = new HashMap<>();
        loadPromocodesFromFile("src/main/java/ru/turandot/codes.csv");
    }

    // Метод для загрузки промокодов из CSV файла
    public void loadPromocodesFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Добавляем промокод со статусом "неиспользованный"
                promocodes.put(line.trim(), false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getAvailablePromocode() {
        for (Map.Entry<String, Boolean> entry : promocodes.entrySet()) {
            if (!entry.getValue()) {
                promocodes.put(entry.getKey(), true);
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isPromocodeUsed(String code) {
        return promocodes.getOrDefault(code, false);
    }

    public void setPromocodeStatus(String code, boolean isUsed) {
        if (promocodes.containsKey(code)) {
            promocodes.put(code, isUsed);
        }
    }
}

public class Bot extends TelegramLongPollingBot {
    private final String auth_token = "6783093196:AAF9vNB7gLtIwLyfxiPVhzE8-KZ_zkWdAZs";

    private HashSet<Long> usersWithDiscounts = new HashSet<>();

    private PromocodesDB pdb = new PromocodesDB();

    @Override
    public String getBotUsername() {
        return "turandot_lingerie_bot";
    }

    @Override
    public String getBotToken() {
        return auth_token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

//            if ("/test".equals(messageText)) {
//                SendMessage message = new SendMessage();
//                message.setChatId(String.valueOf(chatId));
//
//                GetChatMember chatMemberRequest = new GetChatMember();
//                chatMemberRequest.setChatId("-1002008065572");
//                chatMemberRequest.setUserId(update.getMessage().getFrom().getId());
//                ChatMember chatMember = null;
//                try {
//                    chatMember = execute(chatMemberRequest);
//                } catch (TelegramApiException e) {
//                    throw new RuntimeException(e);
//                }
//                String status = chatMember.getStatus();
//                boolean isSubscribed = "member".equals(status) || "administrator".equals(status) || "creator".equals(status);
//
//                if (isSubscribed) {
//                    message.setText("You're in!");
//                }
//                else {
//                    message.setText("You're out!");
//                }
//
//                try {
//                    execute(message); // Отправка сообщения
//                } catch (TelegramApiException e) {
//                    e.printStackTrace();
//                }
//            }

            if ("/start".equals(messageText)) {
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));

                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("Получить скидку");
                button.setCallbackData("get_discount"); // Уникальный идентификатор для обработки нажатия кнопки
                row.add(button);
                buttons.add(row);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                markup.setKeyboard(buttons);
                message.setReplyMarkup(markup);
                message.setText("Хотите получить персональную скидку? Вы должны быть подписаны на наш канал https://t.me/+5vOgH64h5scwZTgy");

                try {
                    execute(message); // Отправка сообщения
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }

        if (update.hasCallbackQuery()) {
            String callData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if ("get_discount".equals(callData)) {
                // Проверка подписки
                GetChatMember chatMemberRequest = new GetChatMember();
                chatMemberRequest.setChatId("-1002008065572");
                chatMemberRequest.setUserId(update.getCallbackQuery().getFrom().getId());
                ChatMember chatMember = null;
                try {
                    chatMember = execute(chatMemberRequest);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                String status = chatMember.getStatus();
                boolean isSubscribed = "member".equals(status) || "administrator".equals(status) || "creator".equals(status);

                // Проверка предыдущих скидок
                boolean hasReceivedDiscount = usersWithDiscounts.contains(update.getCallbackQuery().getFrom().getId());

                if (isSubscribed && !hasReceivedDiscount) {
//                    String promoCode = generatePromoCode(); // Метод для генерации промокода
                    String promoCode = pdb.getAvailablePromocode();
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText("Ваш промокод: " + promoCode);
                    try {
                        execute(message);
                        savePromoCodeUsage(update.getCallbackQuery().getFrom().getId(), promoCode); // Сохранить информацию о выдаче промокода
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else {
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText("Вы не подписаны на канал или уже получили скидку.");
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

//    private String generatePromoCode() {
//        return RandomStringGenerator.generateRandomString(10);
//    }

    private void savePromoCodeUsage(Long id, String promoCode) {
        usersWithDiscounts.add(id);
    }
}
