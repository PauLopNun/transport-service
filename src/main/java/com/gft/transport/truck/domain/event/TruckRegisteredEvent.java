package com.gft.transport.truck.domain.event;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import lombok.Value;

@Value
public class TruckRegisteredEvent {
    TruckId truckId;
    String name;
    Location location;
    int capacity;
    int timestamp;
}
