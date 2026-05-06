package com.gft.transport.delivery.infrastructure.messaging;

import com.gft.transport.delivery.application.usecase.AdvanceTrucks;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeAdvancedListener implements MessageListener {

    private AdvanceTrucks advanceTrucks;
    private Integer lastDay;

    @Override
    @RabbitListener(queues = RabbitMQConfig.TIME_ADVANCED_QUEUE)
    public void onMessage(Message message) {

        String body = new String(message.getBody());


    }

    private Integer calculateDaysAdvanced(){

        return 0;

    }

}