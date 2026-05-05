package com.gft.transport.delivery.domain;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class Delivery {

    private final DeliveryId deliveryId;
    private final UUID shipmentId;
    private final TruckId truckId;
    private final Location origin;
    private final Location destination;
    private final List<DeliveryItem> items;
    private final int assignedAt;
    private final Integer completedAt;

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean isArrived(Location truckLocation) {
        return truckLocation.equals(destination);
    }

    public Delivery complete(int completedAt) {
        return Delivery.builder()
                .deliveryId(this.deliveryId)
                .shipmentId(this.shipmentId)
                .truckId(this.truckId)
                .origin(this.origin)
                .destination(this.destination)
                .items(this.items)
                .assignedAt(this.assignedAt)
                .completedAt(completedAt)
                .build();
    }
}
