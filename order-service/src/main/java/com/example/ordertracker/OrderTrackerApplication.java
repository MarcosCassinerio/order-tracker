package com.example.ordertracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  // habilita @Async en los @EventListener
public class OrderTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderTrackerApplication.class, args);
    }
}
