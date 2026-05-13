package com.gft.transport.truck.infrastructure.messaging.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.delivery.domain.DeliveryItem;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentRequestedMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesAllFieldsFromJson() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        String productId = UUID.randomUUID().toString();
        String json = """
                {
                    "shipmentId": "%s",
                    "originId": "warehouse-north-01",
                    "destinationId": "warehouse-south-03",
                    "items": [{"productId": "%s", "quantity": 6}],
                    "requestedAt": 3
                }
                """.formatted(shipmentId, productId);

        ShipmentRequestedMessage message = objectMapper.readValue(json, ShipmentRequestedMessage.class);

        assertThat(message.shipmentId()).isEqualTo(shipmentId);
        assertThat(message.originId()).isEqualTo("warehouse-north-01");
        assertThat(message.destinationId()).isEqualTo("warehouse-south-03");
        assertThat(message.items()).containsExactly(new DeliveryItem(productId, 6));
        assertThat(message.requestedAt()).isEqualTo(3);
    }

    @Test
    void ignoresUnknownFields() throws Exception {
        String json = """
                {
                    "shipmentId": "%s",
                    "originId": "warehouse-north-01",
                    "destinationId": "warehouse-south-03",
                    "items": [],
                    "requestedAt": 1,
                    "unexpectedField": "should-be-ignored"
                }
                """.formatted(UUID.randomUUID());

        assertThat(objectMapper.readValue(json, ShipmentRequestedMessage.class)).isNotNull();
    }
}
