package com.gft.transport.truck.infrastructure.messaging.dto;

import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TruckRegisteredMessage {

    String truckId;
    String name;
    LocationDto location;
    int capacity;
    int timestamp;

    @Value
    public static class LocationDto {
        int x;
        int y;
    }

    public static TruckRegisteredMessage from(TruckRegisteredEvent event) {
        return TruckRegisteredMessage.builder()
                .truckId(event.getTruckId().value().toString())
                .name(event.getName())
                .location(new LocationDto(event.getLocation().x(), event.getLocation().y()))
                .capacity(event.getCapacity())
                .timestamp(event.getTimestamp())
                .build();
    }
}
