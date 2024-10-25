package com.example.demo.coindesk;

import com.example.demo.currency.CurrencyEntity;
import com.example.demo.currency.CurrencyEntityRepository;
import com.example.demo.config.BaseLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CoinDeskService extends BaseLogger {
    private final WebClient webClient;
    private final MessageSource messageSource;
    private final CurrencyEntityRepository currencyEntityRepository;

    @Autowired
    public CoinDeskService(WebClient.Builder webClientBuilder, MessageSource messageSource, CurrencyEntityRepository currencyEntityRepository) {
        this.webClient = webClientBuilder.baseUrl("https://api.coindesk.com").build();
        this.messageSource = messageSource;
        this.currencyEntityRepository = currencyEntityRepository;
    }

    @Async
    public CompletableFuture<List<CurrencyEntity>> getCurrentPrice() {
        return webClient.get()
                .uri("/v1/bpi/currentprice.json")
                .retrieve()
                .bodyToMono(CoinDeskResponse.class)
                .toFuture()
                .thenCompose(response -> {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(response.getTime().getUpdatedISO());
                    Instant instant = zonedDateTime.toInstant();
                    Timestamp timestamp = Timestamp.from(instant);

                    List<CompletableFuture<CurrencyEntity>> futures = response.getBpi().entrySet().stream()
                                    .map(entry -> {
                                        String key = entry.getKey();
                                        String codeCN = messageSource.getMessage(key, null, Locale.TAIWAN);
                                        CurrencyEntity existingEntity = currencyEntityRepository.findByCode(key);

                                        if (existingEntity != null) {
                                            existingEntity.setUpdated(timestamp);
                                            existingEntity.setRateFloat(entry.getValue().getRateFloat());
                                            existingEntity.setCurrencyNameCN(codeCN);
                                            return CompletableFuture.supplyAsync(() -> currencyEntityRepository.save(existingEntity));
                                        } else {
                                            CurrencyEntity newEntity = new CurrencyEntity();
                                            newEntity.setUpdated(timestamp);
                                            newEntity.setCode(key);
                                            newEntity.setCurrencyNameCN(codeCN);
                                            newEntity.setRateFloat(entry.getValue().getRateFloat());
                                            return CompletableFuture.supplyAsync(() -> currencyEntityRepository.save(newEntity));
                                        }
                                    })
                                    .toList();

                            // 使用 CompletableFuture.allOf() 等待所有操作完成
                            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                    .thenApply(v -> futures.stream()
                                            .map(CompletableFuture::join)
                                            .collect(Collectors.toList()));
                        }
                );
    }

    // 使用 @Scheduled 註解定義定時任務
    @Scheduled(cron = "00 00 00 * * ?")
    public void scheduleTask() {
        getCurrentPrice().thenAccept(currencyEntities -> {
                currencyEntities.forEach(entity -> {
                    logger.debug("midnight auto updated: " + "code:{} name:{} rate:{}", entity.getCode(), entity.getCurrencyNameCN(), entity.getRateFloat());
                });
        });
    }
}
