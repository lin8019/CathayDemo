package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Currency API")
                        .version("1.0.0")
                        .description("Allow you to CRUD Currency Information"));
    }

    @Bean
    public GroupedOpenApi CurrencyApi() {
        return GroupedOpenApi.builder()
                .group("currency")
                .pathsToMatch("/api/currency/**")
                .build();
    }

    @Bean
    public GroupedOpenApi EncryptionApi() {
        return GroupedOpenApi.builder()
                .group("encryption")
                .pathsToMatch("/api/encryption/**")
                .build();
    }
}

