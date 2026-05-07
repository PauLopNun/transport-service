package com.gft.transport.delivery.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.delivery.application.usecase.AdvanceTrucks;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TimeAdvancedListener {

    private final AdvanceTrucks advanceTrucks;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.TIME_ADVANCED_QUEUE)
    public void onMessage(Message message) {
        TimeAdvancedMessage msg = readMessage(message);
        if (msg.daysAdvanced() <= 0) {
            return;
        }
        advanceTrucks.execute(msg.daysAdvanced(), msg.currentDayNumber());
    }

    private TimeAdvancedMessage readMessage(Message message) {
        try {
            return objectMapper.readValue(message.getBody(), TimeAdvancedMessage.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid time.advanced.v1 message", e);
        }
    }
}
