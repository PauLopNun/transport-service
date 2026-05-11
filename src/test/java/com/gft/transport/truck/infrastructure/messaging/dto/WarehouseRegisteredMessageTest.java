package com.gft.transport.truck.infrastructure.messaging.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseRegisteredMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesAllFieldsFromJson() throws Exception {
        String json = """
                {
                    "warehouseId": "warehouse-north-01",
                    "location": { "x": 5, "y": 10 }
                }
                """;

        WarehouseRegisteredMessage message = objectMapper.readValue(json, WarehouseRegisteredMessage.class);

        assertThat(message.warehouseId()).isEqualTo("warehouse-north-01");
        assertThat(message.location().x()).isEqualTo(5);
        assertThat(message.location().y()).isEqualTo(10);
    }

    @Test
    void ignoresUnknownFields() throws Exception {
        String json = """
                {
                    "warehouseId": "warehouse-north-01",
                    "location": { "x": 5, "y": 10 },
                    "unexpectedField": "should-be-ignored"
                }
                """;

        assertThat(objectMapper.readValue(json, WarehouseRegisteredMessage.class)).isNotNull();
    }
}
