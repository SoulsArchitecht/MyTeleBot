package ru.sshibko.TeleBot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@PropertySource("application.properties")
@Data
@EnableScheduling
public class BotConfig {

    @Value("${bot.name)")
    private String botName;

    @Value("${bot.key}")
    private String token;

    @Value("${bot.owner.chatid}")
    private Long ownerChatId;
}
