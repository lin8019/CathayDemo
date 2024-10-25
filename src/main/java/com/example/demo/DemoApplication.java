package com.example.demo;

import com.example.demo.coindesk.CoinDeskService;
import com.example.demo.config.BaseLogger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.example.demo")
@EnableAsync
public class DemoApplication extends BaseLogger {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(CoinDeskService coinDeskService) {
        return args -> {
            logger.debug("app init running call api");
            coinDeskService.getCurrentPrice();
        };
    }
}
