package com.example.demo.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig extends BaseLogger {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(logRequest())
                .filter(logResponse());
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) ->
                    values.forEach(value -> logger.info("{}: {}", name, value))
            );
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            logger.debug("Response Status: {}", clientResponse.statusCode());
            clientResponse.headers().asHttpHeaders().forEach((name, values) ->
                    values.forEach(value -> logger.info("{}: {}", name, value))
            );

            return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        // 在這裡記錄完整的 body
                        logger.debug("Response Body: {}", body);
                        // 重新構造一個新的 ClientResponse，將 body 放回去
                        ClientResponse newResponse = ClientResponse.from(clientResponse)
                                .body(body) // 把原本的 body 放回去
                                .build();
                        return Mono.just(newResponse);
                    });
        });
    }
}
