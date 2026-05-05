package com.gft.transport.truck.application.dto;

import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;

public record TruckResponse(
        String truckId,
        String name,
        LocationDto location,
        TruckStatus status
) {
    public record LocationDto(int x, int y) {}

    public static TruckResponse from(Truck truck) {
        return new TruckResponse(
                truck.getTruckId().value().toString(),
                truck.getName(),
                new LocationDto(truck.getLocation().x(), truck.getLocation().y()),
                truck.getStatus()
        );
    }
}
