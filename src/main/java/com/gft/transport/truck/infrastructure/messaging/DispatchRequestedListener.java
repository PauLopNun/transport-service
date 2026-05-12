package com.gft.transport.truck.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.truck.application.usecase.AssignTruck;
import com.gft.transport.truck.application.usecase.AssignTruckCommand;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.messaging.dto.ShipmentRequestedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchRequestedListener {

    private final AssignTruck assignTruck;
    private final LocationResolver locationResolver;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_REQUESTED_QUEUE)
    public void onMessage(Message message) {
        ShipmentRequestedMessage msg = parseMessage(message);

        AssignTruckCommand command = new AssignTruckCommand(
                msg.shipmentId(),
                locationResolver.resolve(msg.originId()),
                locationResolver.resolve(msg.destinationId()),
                msg.items(),
                msg.requestedAt()
        );

        try {
            assignTruck.execute(command);
        } catch (NoTruckAvailableException e) {
            log.warn("Shipment {} discarded: {}", msg.shipmentId(), e.getMessage());
        }
    }

    private ShipmentRequestedMessage parseMessage(Message message) {
        try {
            JsonNode node = objectMapper.readTree(message.getBody());
            if (node.isTextual()) {
                return objectMapper.readValue(node.asText(), ShipmentRequestedMessage.class);
            }
            return objectMapper.treeToValue(node, ShipmentRequestedMessage.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid shipment.requested.v1 message", e);
        }
    }
}