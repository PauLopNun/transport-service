package com.gft.transport.delivery.domain.event;

import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
public class DeliveryCompletedEvent {
    UUID shipmentId;
    TruckId truckId;
    List<DeliveryItem> items;
    Location location;
    int completedAt;
    Instant timestamp;
}
