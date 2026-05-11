package com.gft.transport.truck.infrastructure.messaging.dto;

import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TruckStatusChangedMessage {

    String truckId;
    String oldStatus;
    String newStatus;
    LocationDto position;
    int currentLoad;
    int capacity;
    int timestamp;
    String reason;

    @Value
    public static class LocationDto {
        int x;
        int y;
    }

    public static TruckStatusChangedMessage from(TruckStatusChangedEvent event) {
        return TruckStatusChangedMessage.builder()
                .truckId(event.getTruckId().value().toString())
                .oldStatus(event.getOldStatus() != null ? event.getOldStatus().name() : null)
                .newStatus(event.getNewStatus().name())
                .position(new LocationDto(event.getPosition().x(), event.getPosition().y()))
                .currentLoad(event.getCurrentLoad())
                .capacity(event.getCapacity())
                .timestamp(event.getTimestamp())
                .reason(event.getReason())
                .build();
    }
}
