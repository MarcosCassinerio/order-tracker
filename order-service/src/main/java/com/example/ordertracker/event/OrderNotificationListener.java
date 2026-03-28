package com.example.ordertracker.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * PATRÓN OBSERVER — Listener 1 de N
 *
 * Simula el envío de notificaciones al cliente.
 * En producción: llamaría a un EmailService o publicaría a Kafka.
 *
 * @Async: corre en un thread del pool de Spring, no bloquea el request HTTP.
 * El cliente recibe su 201 Created de inmediato; el email se envía en background.
 */
@Component
@Slf4j
public class OrderNotificationListener {

    @EventListener
    @Async
    public void onOrderPlaced(OrderPlacedEvent event) {
        var order = event.order();
        log.info("[NOTIF] Enviando confirmación a {} — Pedido #{} por ${}",
                order.getContactEmail(),
                order.getId(),
                order.getTotalAmount());

        // En producción: emailService.send(...) o kafkaTemplate.send(...)
        // Por ahora simulamos con un sleep para que se note que es async
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        log.info("[NOTIF] Email enviado para pedido #{}", order.getId());
    }
}
