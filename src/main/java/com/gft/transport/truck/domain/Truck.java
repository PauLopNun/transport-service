package com.gft.transport.truck.domain;

import com.gft.transport.delivery.domain.DeliveryId;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Truck {

    private final TruckId truckId;
    private final String name;
    private final Location location;
    private final TruckStatus status;
    private final int capacity;
    private final int currentLoad;
    private final List<DeliveryId> deliveryIds;

    public int remainingCapacity() {
        return capacity - currentLoad;
    }

    public boolean canAccept(int items) {
        return items <= remainingCapacity();
    }
}
