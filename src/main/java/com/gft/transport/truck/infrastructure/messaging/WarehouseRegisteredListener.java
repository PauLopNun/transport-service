package com.gft.transport.truck.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.messaging.dto.WarehouseRegisteredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseRegisteredListener {

    private final LocationResolver locationResolver;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_REGISTERED_QUEUE)
    public void onMessage(Message message) {
        WarehouseRegisteredMessage msg = parseMessage(message);
        locationResolver.register(msg.warehouseId(), new Location(msg.location().x(), msg.location().y()));
        log.info("Warehouse registered: id={} location=({},{})", msg.warehouseId(), msg.location().x(), msg.location().y());
    }

    private WarehouseRegisteredMessage parseMessage(Message message) {
        try {
            JsonNode node = objectMapper.readTree(message.getBody());
            if (node.isTextual()) {
                return objectMapper.readValue(node.asText(), WarehouseRegisteredMessage.class);
            }
            return objectMapper.treeToValue(node, WarehouseRegisteredMessage.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid warehouse.registered.v1 message", e);
        }
    }
}
