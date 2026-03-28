package com.example.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el exchange, la queue y el binding.
 * Spring AMQP los crea en RabbitMQ si no existen al arrancar.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE   = "orders.exchange";
    public static final String QUEUE      = "orders.placed.queue";
    public static final String ROUTING_KEY = "order.placed";

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue ordersPlacedQueue() {
        return new Queue(QUEUE, true); // durable: sobrevive reinicios de RabbitMQ
    }

    @Bean
    Binding binding(Queue ordersPlacedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(ordersPlacedQueue).to(ordersExchange).with(ROUTING_KEY);
    }

    // Serializa mensajes como JSON en vez del formato binario Java por defecto
    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
