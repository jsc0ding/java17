package com.example.demo1;

import com.example.demo1.bot.MyBot;
import com.example.demo1.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class Demo1Application {

    @Bean
    public TelegramBotsApi telegramBotsApi(MyBot myBot) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(myBot);
        return telegramBotsApi;
    }

    @Bean
    public MyBot myBot(@Value("${bot.token}") String botToken, UserRepository userRepository) {
        return new MyBot(botToken, userRepository);
    }

    public static void main(String[] args) {
        SpringApplication.run(Demo1Application.class, args);
    }
}