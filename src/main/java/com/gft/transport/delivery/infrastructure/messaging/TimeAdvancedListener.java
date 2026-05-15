package com.gft.transport.delivery.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.delivery.application.usecase.AdvanceTrucks;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeAdvancedListener {

    private final AdvanceTrucks advanceTrucks;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.TIME_ADVANCED_QUEUE)
    public void onMessage(Message message) {
        TimeAdvancedMessage msg = readMessage(message);
        log.info("Time tick received: currentDay={} daysAdvanced={}", msg.currentDayNumber(), msg.daysAdvanced());
        if (msg.daysAdvanced() <= 0) {
            log.debug("Skipping advance: daysAdvanced={}", msg.daysAdvanced());
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
