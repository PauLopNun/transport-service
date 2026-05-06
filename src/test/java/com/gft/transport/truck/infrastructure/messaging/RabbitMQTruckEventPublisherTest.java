package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckRegisteredMessage;
import com.gft.transport.truck.infrastructure.messaging.dto.TruckStatusChangedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQTruckEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQTruckEventPublisher publisher;

    @Test
    void publishesStatusChangedEventToCorrectExchangeAndRoutingKey() {
        TruckId truckId = TruckId.generate();
        TruckStatusChangedEvent event = new TruckStatusChangedEvent(
                truckId,
                TruckStatus.AVAILABLE,
                TruckStatus.IN_TRANSIT,
                new Location(3, 7),
                6, 10, 3, "DISPATCHED"
        );

        publisher.publish(event);

        ArgumentCaptor<TruckStatusChangedMessage> captor = ArgumentCaptor.forClass(TruckStatusChangedMessage.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TRUCKS_EXCHANGE),
                org.mockito.ArgumentMatchers.eq("truck.status.changed.v1"),
                captor.capture()
        );

        TruckStatusChangedMessage sent = captor.getValue();
        assertThat(sent.getTruckId()).isEqualTo(truckId.value().toString());
        assertThat(sent.getOldStatus()).isEqualTo("AVAILABLE");
        assertThat(sent.getNewStatus()).isEqualTo("IN_TRANSIT");
        assertThat(sent.getReason()).isEqualTo("DISPATCHED");
    }

    @Test
    void publishesRegisteredEventToCorrectExchangeAndRoutingKey() {
        TruckId truckId = TruckId.generate();
        TruckRegisteredEvent event = new TruckRegisteredEvent(
                truckId,
                "Truck-1",
                new Location(0, 0),
                10,
                0
        );

        publisher.publish(event);

        ArgumentCaptor<TruckRegisteredMessage> captor = ArgumentCaptor.forClass(TruckRegisteredMessage.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TRUCKS_EXCHANGE),
                org.mockito.ArgumentMatchers.eq("truck.registered.v1"),
                captor.capture()
        );

        TruckRegisteredMessage sent = captor.getValue();
        assertThat(sent.getTruckId()).isEqualTo(truckId.value().toString());
        assertThat(sent.getName()).isEqualTo("Truck-1");
        assertThat(sent.getCapacity()).isEqualTo(10);
        assertThat(sent.getTimestamp()).isEqualTo(0);
    }
}
