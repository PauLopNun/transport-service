package com.gft.transport.truck.domain.event;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import lombok.Value;

import java.time.Instant;

@Value
public class TruckPositionUpdatedEvent {
    TruckId truckId;
    Location oldPosition;
    Location newPosition;
    Instant timestamp;
}
