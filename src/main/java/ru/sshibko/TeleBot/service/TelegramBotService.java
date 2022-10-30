package ru.sshibko.TeleBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.sshibko.TeleBot.config.BotConfig;
import ru.sshibko.TeleBot.model.entity.Ad;
import ru.sshibko.TeleBot.model.entity.User;
import ru.sshibko.TeleBot.model.repository.AdRepository;
import ru.sshibko.TeleBot.model.repository.UserRepository;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    @Autowired
    private final BotConfig config;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdRepository adRepository;
    private static final String HELP_TEXT = "This bot is designed to cheer sad people \n\n"
            + "You can execute commands from the main menu at the left angle or by typing command:\n\n"
            + "Type /start to see a welcome message\n\n"
            + "Type /help to see help information\n\n"
            + "Type /cheerme to get fun\n\n"
            + "Type /userdata to get your personal information\n\n"
            + "Type /deletedata to delete your personal information\n\n"
            + "Type /settings to set your preferences\n\n"
            + "Type /register for registration";

    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";
    private static final String LOG_ERROR_TEXT = "Error occurred: ";

    @Autowired
    public TelegramBotService(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/userdata", "get user information"));
        listOfCommands.add(new BotCommand("/deletedata", "delete user information"));
        listOfCommands.add(new BotCommand("/help", "help info"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/register", "for registration"));
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

            if (messageText.contains("/send") && chatId == config.getOwnerChatId()) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(
                        messageText.indexOf(" ")));
                var users= userRepository.findAll();
                //Iterable<User> users = userRepository.findAll();
                for (User user: users) {
                    sendMessage(user.getChatId(), textToSend);
                }
            } else {
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
                    case "/userdata":
                        sendMessage(chatId, update.getMessage().getChat().getFirstName() + "\n"
                                + update.getMessage().getChat().getLastName() + "\n"
                                + update.getMessage().getChat().getUserName() + "\n"
                                + update.getMessage().getChat().getPhoto() + "\n"
                                + update.getMessage().getChat().getBio());
                        break;
                    case "/register":
                        register(chatId);
                    default:
                        sendMessage(chatId, "Sorry, command was not recognized");
                }
            }


        } else if (update.hasCallbackQuery()) {
            String callbackDate = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackDate.equals(YES_BUTTON)) {
                String text = "You pressed YES button";
                executeMessageTextEdit(chatId, messageId, text);

            } else if (callbackDate.equals(NO_BUTTON)) {
                String text = "You pressed NO button";
                executeMessageTextEdit(chatId, messageId, text);
            }
        }

    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want to register?");

        message.setReplyMarkup(applyInlineKeyboardMarkup());

        executeSendMessage(message);
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
        sendMessage(chatId, answer);
        log.info("Replied to user " + name);
    }

    private void cheerMeCommandReceived(long chatId, String name) {
        String answer = "Get fun, " + name + "!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = prepareMessage(chatId, textToSend);

        //message.setReplyMarkup(applyKeyboardKeys());

        executeSendMessage(message);
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

    private ReplyKeyboardMarkup applyKeyboardKeys() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("about a weather");
        row1.add("get random joke");
        row1.add("check up on");
        keyboardRows.add(row1);
        KeyboardRow row2 = new KeyboardRow();
        row2.add("register");
        row2.add("check my personal data");
        row2.add("delete my personal data");
        keyboardRows.add(row2);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private InlineKeyboardMarkup applyInlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var buttonYes = new InlineKeyboardButton();
        buttonYes.setText("Yes");
        buttonYes.setCallbackData(YES_BUTTON);
        var buttonNo = new InlineKeyboardButton();
        buttonNo.setText("No");
        buttonNo.setCallbackData(NO_BUTTON);

        rowInLine.add(buttonYes);
        rowInLine.add(buttonNo);

        rowsInLine.add(rowInLine);

        inlineKeyboardMarkup.setKeyboard(rowsInLine);

        return inlineKeyboardMarkup;
    }

    private void executeMessageTextEdit(long chatId, long messageId, String text) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        executeEditMessage(message);
    }

    private void executeSendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(LOG_ERROR_TEXT + e.getMessage());
        }
    }

    private void executeEditMessage(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(LOG_ERROR_TEXT + e.getMessage());
        }
    }

    private SendMessage prepareMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        return message;
    }

    @Scheduled(cron = "${bot.cron.scheduler}" ) // seconds, minutes, days, month, dayOfTheWeek
    private void sendAds() {
        var ads = adRepository.findAll();
        var users = userRepository.findAll();

        for (Ad ad: ads) {
            for (User user: users) {
               SendMessage message = prepareMessage(user.getChatId(), ad.getAdText());
               executeSendMessage(message);
            }
        }
    }
}
