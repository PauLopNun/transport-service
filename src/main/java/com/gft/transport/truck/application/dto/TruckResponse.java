package com.gft.transport.truck.application.dto;

import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Truck state as returned by the REST API")
public record TruckResponse(
        @Schema(description = "Truck UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String truckId,
        @Schema(description = "Display name", example = "Truck 01") String name,
        @Schema(description = "Current grid position") LocationDto location,
        @Schema(description = "Operational status") TruckStatus status
) {
    @Schema(description = "Position on the 2D grid")
    public record LocationDto(
            @Schema(example = "3") int x,
            @Schema(example = "5") int y
    ) {}

    public static TruckResponse from(Truck truck) {
        return new TruckResponse(
                truck.getTruckId().value().toString(),
                truck.getName(),
                new LocationDto(truck.getLocation().x(), truck.getLocation().y()),
                truck.getStatus()
        );
    }
}
