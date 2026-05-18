package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.event.TruckDeletedEvent;
import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckDeletedMessage;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckPositionUpdatedMessage;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckRegisteredMessage;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckStatusChangedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
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
        log.debug("Published truck.registered.v1: truckId={}", event.getTruckId().value());
    }

    @Override
    public void publish(TruckStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.status.changed.v1",
                TruckStatusChangedMessage.from(event)
        );
        log.debug("Published truck.status.changed.v1: truckId={} reason={}", event.getTruckId().value(), event.getReason());
    }

    @Override
    public void publish(TruckPositionUpdatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.position.updated.v1",
                TruckPositionUpdatedMessage.from(event)
        );
        log.debug("Published truck.position.updated.v1: truckId={}", event.getTruckId().value());
    }

    @Override
    public void publish(TruckDeletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.deleted.v1",
                TruckDeletedMessage.from(event)
        );
        log.debug("Published truck.deleted.v1: truckId={}", event.getTruckId().value());
    }
}
