package com.example.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient inventoryClient(
            @Value("${services.inventory.url}") String inventoryUrl) {
        return WebClient.builder()
                .baseUrl(inventoryUrl)
                .build();
    }
}
