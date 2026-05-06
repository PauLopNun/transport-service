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

    private Integer lastDay;

    @RabbitListener(queues = RabbitMQConfig.TIME_ADVANCED_QUEUE)
    public synchronized void onMessage(Message message) {
        int currentDay = readCurrentDay(message);

        if (lastDay == null) {
            lastDay = currentDay;
            return;
        }

        int daysAdvanced = calculateDaysAdvanced(currentDay);
        if (daysAdvanced <= 0) {
            return;
        }

        lastDay = currentDay;
        advanceTrucks.execute(daysAdvanced, currentDay);
    }

    int calculateDaysAdvanced(int currentDay) {
        return currentDay - lastDay;
    }

    private int readCurrentDay(Message message) {
        try {
            return objectMapper.readValue(message.getBody(), TimeAdvancedMessage.class).currentDay();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid time.advanced.v1 message", e);
        }
    }

}
