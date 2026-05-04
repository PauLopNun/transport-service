package com.gft.transport.truck.application.dto;

import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;

public record TruckResponse(
        String truckId,
        String name,
        int locationX,
        int locationY,
        TruckStatus status
) {
    public static TruckResponse from(Truck truck) {
        return new TruckResponse(
                truck.getTruckId().value().toString(),
                truck.getName(),
                truck.getLocation().x(),
                truck.getLocation().y(),
                truck.getStatus()
        );
    }
}
