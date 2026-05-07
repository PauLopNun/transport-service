package com.gft.transport.delivery.infrastructure.messaging;

import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventPublisher implements com.gft.transport.delivery.application.port.out.DeliveryEventPublisher {

    @Override
    public void publish(DeliveryCompletedEvent event) {
        // TODO: implement delivery.completed.v1 publishing via RabbitMQ
    }
}