package ru.sshibko.TeleBot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.sshibko.TeleBot.config.BotConfig;
import ru.sshibko.TeleBot.model.entity.User;
import ru.sshibko.TeleBot.model.repository.UserRepository;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    private final BotConfig config;

    @Autowired
    private UserRepository userRepository;
    private static final String HELP_TEXT = "This bot is designed to cheer sad people \n\n"
            + "You can execute commands from the main menu at the left angle or by typing command:\n\n"
            + "Type /start to see a welcome message\n\n"
            + "Type /help to see help information\n\n"
            + "Type /cheerme to get fun\n\n"
            + "Type /userdata to get your personal information\n\n"
            + "Type /deletedata to delete your personal information\n\n"
            + "Type /settings to set your preferences";

    @Autowired
    public TelegramBotService(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/userdata", "get user information"));
        listOfCommands.add(new BotCommand("/deletedata", "delete user information"));
        listOfCommands.add(new BotCommand("/help", "help info"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                case "/cheerme":
                    cheerMeCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                default:
                    sendMessage(chatId, "Sorry, command was not recognized");
            }
        }

    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";
        sendMessage(chatId, answer);
        log.info("Replied to user " + name);
    }

    private void cheerMeCommandReceived(long chatId, String name) {
        String answer = "Get fun, " + name + "!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            Long chatId = message.getChatId();
            Chat chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());

            userRepository.save(user);
            log.info("user " + user + " saved");
        }
    }
}
