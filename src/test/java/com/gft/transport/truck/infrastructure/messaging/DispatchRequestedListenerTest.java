package com.gft.transport.truck.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.truck.application.usecase.AssignTruck;
import com.gft.transport.truck.application.usecase.AssignTruckCommand;
import com.gft.transport.truck.domain.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.gft.transport.truck.domain.exception.NoTruckAvailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchRequestedListenerTest {

    @Mock
    private AssignTruck assignTruck;

    @Mock
    private LocationResolver locationResolver;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private DispatchRequestedListener listener;

    @Test
    void dispatchesTruckWhenValidMessageReceived() {
        UUID shipmentId = UUID.randomUUID();
        Location origin = new Location(0, 10);
        Location destination = new Location(10, 0);

        when(locationResolver.resolve("warehouse-north-01")).thenReturn(origin);
        when(locationResolver.resolve("warehouse-south-03")).thenReturn(destination);

        Message message = buildMessage("""
                {
                    "shipmentId": "%s",
                    "originId": "warehouse-north-01",
                    "destinationId": "warehouse-south-03",
                    "items": [{"materialType": "wood", "quantity": 6}],
                    "requestedAt": 3
                }
                """.formatted(shipmentId));

        listener.onMessage(message);

        ArgumentCaptor<AssignTruckCommand> captor = ArgumentCaptor.forClass(AssignTruckCommand.class);
        verify(assignTruck).execute(captor.capture());

        AssignTruckCommand command = captor.getValue();
        assertThat(command.shipmentId()).isEqualTo(shipmentId);
        assertThat(command.origin()).isEqualTo(origin);
        assertThat(command.destination()).isEqualTo(destination);
        assertThat(command.items()).containsExactly(new DeliveryItem("wood", 6));
        assertThat(command.requestedAt()).isEqualTo(3);
    }

    @Test
    void discardsShipmentWhenNoTruckIsAvailable() {
        UUID shipmentId = UUID.randomUUID();
        when(locationResolver.resolve("warehouse-north-01")).thenReturn(new Location(0, 10));
        when(locationResolver.resolve("warehouse-south-03")).thenReturn(new Location(10, 0));
        doThrow(new NoTruckAvailableException("No available truck found for the requested shipment"))
                .when(assignTruck).execute(any());

        Message message = buildMessage("""
                {
                    "shipmentId": "%s",
                    "originId": "warehouse-north-01",
                    "destinationId": "warehouse-south-03",
                    "items": [{"materialType": "wood", "quantity": 6}],
                    "requestedAt": 3
                }
                """.formatted(shipmentId));

        assertThatCode(() -> listener.onMessage(message)).doesNotThrowAnyException();
    }

    @Test
    void throwsExceptionWhenMessageIsInvalid() {
        Message message = buildMessage("not-json");

        assertThatThrownBy(() -> listener.onMessage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid shipment.requested.v1 message");

        verifyNoInteractions(assignTruck);
    }

    @Test
    void dispatchesTruckWhenMessageIsDoubleEncoded() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        Location origin = new Location(0, 10);
        Location destination = new Location(10, 0);

        when(locationResolver.resolve("warehouse-north-01")).thenReturn(origin);
        when(locationResolver.resolve("warehouse-south-03")).thenReturn(destination);

        String innerJson = """
                {
                    "shipmentId": "%s",
                    "originId": "warehouse-north-01",
                    "destinationId": "warehouse-south-03",
                    "items": [{"materialType": "wood", "quantity": 6}],
                    "requestedAt": 3
                }
                """.formatted(shipmentId);
        Message message = buildMessage(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(innerJson));

        listener.onMessage(message);

        ArgumentCaptor<AssignTruckCommand> captor = ArgumentCaptor.forClass(AssignTruckCommand.class);
        verify(assignTruck).execute(captor.capture());

        AssignTruckCommand command = captor.getValue();
        assertThat(command.shipmentId()).isEqualTo(shipmentId);
        assertThat(command.origin()).isEqualTo(origin);
        assertThat(command.destination()).isEqualTo(destination);
        assertThat(command.items()).containsExactly(new DeliveryItem("wood", 6));
        assertThat(command.requestedAt()).isEqualTo(3);
    }

    private Message buildMessage(String json) {
        return MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
    }
}
