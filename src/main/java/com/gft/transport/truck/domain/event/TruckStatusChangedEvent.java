package com.gft.transport.truck.domain.event;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import lombok.Value;

import java.time.Instant;

@Value
public class TruckStatusChangedEvent {
    TruckId truckId;
    TruckStatus oldStatus;
    TruckStatus newStatus;
    Location position;
    int currentLoad;
    int capacity;
    Instant timestamp;
    String reason;
}
