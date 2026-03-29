package com.example.notification.listener;

import com.example.notification.config.RabbitMQConfig;
import com.example.notification.dto.OrderPlacedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * PATRÓN OBSERVER — consumer side
 *
 * @RabbitListener connects to RabbitMQ at startup and listens continuously.
 * Each message delivered to the queue triggers onOrderPlaced automatically.
 *
 * In production: would call an EmailService, push notification service, etc.
 */
@Component
@Slf4j
public class OrderNotificationListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onOrderPlaced(OrderPlacedMessage message) {
        log.info("[NOTIF] Sending confirmation to {} — Order #{} for ${}",
                message.contactEmail(),
                message.orderId(),
                message.totalAmount());

        // In production: emailService.send(...) or pushNotificationService.send(...)

        log.info("[NOTIF] Notification sent for order #{}", message.orderId());
    }
}
