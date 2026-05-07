package com.gft.transport.delivery.infrastructure.messaging;

import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;
import com.gft.transport.delivery.infrastructure.messaging.dto.DeliveryCompletedMessage;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
/**
 * Trello: TRK-43 / TRK-252 - RabbitMQ publisher for delivery.completed.v1.
 */
public class DeliveryEventPublisher implements com.gft.transport.delivery.application.port.out.DeliveryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DeliveryCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SHIPMENTS_EXCHANGE,
                "delivery.completed.v1",
                DeliveryCompletedMessage.from(event)
        );
    }
}
