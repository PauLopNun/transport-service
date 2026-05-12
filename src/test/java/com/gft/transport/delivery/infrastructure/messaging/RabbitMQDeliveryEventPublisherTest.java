package com.gft.transport.delivery.infrastructure.messaging;

import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;
import com.gft.transport.delivery.infrastructure.messaging.dto.DeliveryCompletedMessage;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQDeliveryEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQDeliveryEventPublisher publisher;

    @Test
    void publishesDeliveryCompletedEventToCorrectExchangeAndRoutingKey() {
        UUID productId = UUID.randomUUID();
        DeliveryCompletedEvent event = new DeliveryCompletedEvent(
                UUID.randomUUID(),
                new TruckId(UUID.randomUUID()),
                List.of(new DeliveryItem(productId, 6)),
                new Location(8, 2),
                5
        );

        publisher.publish(event);

        ArgumentCaptor<DeliveryCompletedMessage> captor = ArgumentCaptor.forClass(DeliveryCompletedMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.SHIPMENTS_EXCHANGE),
                eq("delivery.completed.v1"),
                captor.capture()
        );

        DeliveryCompletedMessage message = captor.getValue();
        assertThat(message.getShipmentId()).isEqualTo(event.getShipmentId().toString());
        assertThat(message.getTruckId()).isEqualTo(event.getTruckId().value().toString());
        assertThat(message.getCompletedAt()).isEqualTo(5);
        assertThat(message.getItems()).hasSize(1);
        assertThat(message.getItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(message.getLocation().getX()).isEqualTo(8);
        assertThat(message.getLocation().getY()).isEqualTo(2);
    }
}
