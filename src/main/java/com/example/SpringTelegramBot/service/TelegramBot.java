package com.example.SpringTelegramBot.service;

import com.example.SpringTelegramBot.config.BotConfig;
import com.example.SpringTelegramBot.model.Ads;
import com.example.SpringTelegramBot.model.Joke;
import com.example.SpringTelegramBot.model.User;
import com.example.SpringTelegramBot.model.repository.AdsRepository;
import com.example.SpringTelegramBot.model.repository.JokeRepository;
import com.example.SpringTelegramBot.model.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private JokeRepository jokeRepository;
    private final BotConfig botConfig;
    private static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /deletedata was deleted stored about yourself\n\n" +
            "Type /register to registration\n\n" +
            "Type /joke to get random joke\n\n" +
            "Type /help to see this message again";

    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";

    private static final String ERROR = "Error occurred: ";

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/joke", "get random joke"));
        listOfCommands.add(new BotCommand("/register", "registration"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (message.contains("/send") && chatId == botConfig.getOwnerId()) {
                var textToSend = EmojiParser.parseToUnicode(message.substring(message.indexOf(" ")));
                var users = userRepository.findAll();
                users.forEach(user -> prepareAndSendMessage(user.getChatId(), textToSend));
            } else {
                switch (message) {
                    case "/start" -> {
                        registerUser(update.getMessage());
                        showStart(chatId, update.getMessage().getChat().getFirstName());
                    }
                    case "/joke" -> sendMessage(chatId, getRandomJoke().toString());
                    case "/help" -> prepareAndSendMessage(chatId, HELP_TEXT);
                    case "/register" -> register(chatId);
                    case "/mydata" -> {
                        User user = userRepository.findById(chatId).stream().findFirst().orElse(null);
                        assert user != null;
                        prepareAndSendMessage(chatId, "Your data: ");
                        prepareAndSendMessage(chatId, user.toString());
                        log.info("User check data " + user);
                    }
                    case "/deletedata" -> {
                        userRepository.delete(Objects.requireNonNull(userRepository.findById(chatId).stream().findFirst().orElse(null)));
                        prepareAndSendMessage(chatId, "Your data was deleted");
                        log.info("User deleted data " + chatId);
                    }
                    default -> prepareAndSendMessage(chatId, "Sorry, this command was not recognized");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callBackData.equals(YES_BUTTON)) {
                String text = "You pressed YES button";
                executeEditMessageText(text, chatId, messageId);
            } else if (callBackData.equals(NO_BUTTON)) {
                String text = "You pressed NO button";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want register?");
        log.info("User check register command");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    private void showStart(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("/start");
        row.add("/help");
        row.add("/joke");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("/register");
        row.add("/mydata");
        row.add("/deletedata");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        executeMessage(message);
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();
        for (Ads ad : ads) {
            users.forEach(user -> prepareAndSendMessage(user.getChatId(), ad.getAdText()));
        }
    }

    private Joke getRandomJoke() {
        if(jokeRepository.findById(1).stream().findFirst().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                TypeFactory typeFactory = objectMapper.getTypeFactory();
                List<Joke> jokeList = objectMapper.readValue(new File("db/stupidstuff.json"),
                        typeFactory.constructCollectionType(List.class, Joke.class));
                jokeRepository.saveAll(jokeList);
            } catch (Exception e) {
                log.error(Arrays.toString(e.getStackTrace()));
            }
        }
        return jokeRepository.findById(randomInt(1, 3773)).stream().findFirst().orElse(null);
    }

    private int randomInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }
}
