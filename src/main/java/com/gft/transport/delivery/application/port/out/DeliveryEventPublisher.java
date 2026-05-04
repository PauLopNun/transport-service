package com.gft.transport.delivery.application.port.out;

import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;

public interface DeliveryEventPublisher {
    void publish(DeliveryCompletedEvent event);
}
