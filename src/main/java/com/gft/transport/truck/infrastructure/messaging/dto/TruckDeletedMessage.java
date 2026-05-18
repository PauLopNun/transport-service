package com.gft.transport.truck.infrastructure.messaging.dto;

import com.gft.transport.truck.domain.event.TruckDeletedEvent;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TruckDeletedMessage {

    String truckId;

    public static TruckDeletedMessage from(TruckDeletedEvent event) {
        return TruckDeletedMessage.builder()
                .truckId(event.getTruckId().value().toString())
                .build();
    }
}
