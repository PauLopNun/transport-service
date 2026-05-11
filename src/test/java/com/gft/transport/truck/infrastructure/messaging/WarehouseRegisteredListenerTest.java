package com.gft.transport.truck.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.truck.domain.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WarehouseRegisteredListenerTest {

    @Mock
    private LocationResolver locationResolver;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private WarehouseRegisteredListener listener;

    @Test
    void registersWarehouseLocationWhenValidMessageReceived() {
        Message message = buildMessage("""
                {
                    "warehouseId": "warehouse-north-01",
                    "location": { "x": 5, "y": 10 }
                }
                """);

        listener.onMessage(message);

        verify(locationResolver).register("warehouse-north-01", new Location(5, 10));
    }

    @Test
    void throwsExceptionWhenMessageIsInvalid() {
        Message message = buildMessage("not-json");

        assertThatThrownBy(() -> listener.onMessage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid warehouse.registered.v1 message");
    }

    @Test
    void registersWarehouseLocationWhenMessageIsDoubleEncoded() throws Exception {
        String innerJson = "{\"warehouseId\": \"warehouse-north-01\", \"location\": {\"x\": 5, \"y\": 10}}";
        Message message = buildMessage(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(innerJson));

        listener.onMessage(message);

        verify(locationResolver).register("warehouse-north-01", new Location(5, 10));
    }

    private Message buildMessage(String json) {
        return MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
    }
}
