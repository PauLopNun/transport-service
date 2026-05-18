package com.gft.transport.truck.domain;

import com.gft.transport.delivery.domain.DeliveryId;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
public class Truck {

    private final TruckId truckId;
    private final String name;
    private final Location location;
    private final TruckStatus status;
    private final int capacity;
    private final int currentLoad;
    @Builder.Default
    private final int speed = 1;
    private final List<DeliveryId> deliveryIds;
    @Builder.Default
    private final boolean pendingDeletion = false;

    public int remainingCapacity() {
        return capacity - currentLoad;
    }

    public boolean canAccept(int itemCount) {
        return itemCount <= remainingCapacity();
    }
}
