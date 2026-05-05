package com.gft.transport.truck.application.port.out;

import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;

public interface TruckEventPublisher {
    void publish(TruckRegisteredEvent event);
    void publish(TruckStatusChangedEvent event);
    void publish(TruckPositionUpdatedEvent event);
}
