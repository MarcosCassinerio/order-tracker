package com.example.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Must mirror the exchange/queue/routing key declared in order-service.
 * Both services declare them so either can start first without errors.
 * RabbitMQ ignores duplicate declarations if the config is identical.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE    = "orders.exchange";
    public static final String QUEUE       = "orders.placed.queue";
    public static final String ROUTING_KEY = "order.placed";

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue ordersPlacedQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    Binding binding(Queue ordersPlacedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(ordersPlacedQueue).to(ordersExchange).with(ROUTING_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
