package com.gft.transport.truck.infrastructure.messaging.dto;

import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
/**
 * Trello: TRK-41 / TRK-241 - Publish truck.position.updated.v1.
 */
public class TruckPositionUpdatedMessage {

    String truckId;
    LocationDto location;

    @Value
    public static class LocationDto {
        int x;
        int y;
    }

    public static TruckPositionUpdatedMessage from(TruckPositionUpdatedEvent event) {
        return TruckPositionUpdatedMessage.builder()
                .truckId(event.getTruckId().value().toString())
                .location(new LocationDto(event.getLocation().x(), event.getLocation().y()))
                .build();
    }
}
