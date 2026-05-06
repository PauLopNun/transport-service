package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckRegisteredMessage;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckStatusChangedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQTruckEventPublisher implements TruckEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(TruckRegisteredEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.registered.v1",
                TruckRegisteredMessage.from(event)
        );
    }

    @Override
    public void publish(TruckStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.status.changed.v1",
                TruckStatusChangedMessage.from(event)
        );
    }

    @Override
    public void publish(TruckPositionUpdatedEvent event) {
        // implemented in TRK-41
    }
}
