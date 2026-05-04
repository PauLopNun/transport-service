package com.gft.transport.delivery.domain.event;

import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.truck.domain.TruckId;
import lombok.Value;

import java.time.Instant;

@Value
public class DeliveryCompletedEvent {
    DeliveryId deliveryId;
    TruckId truckId;
    int completedAt;
    Instant timestamp;
}
