package com.gft.transport.truck.domain.event;

import com.gft.transport.truck.domain.TruckId;
import lombok.Value;

@Value
public class TruckDeletedEvent {
    TruckId truckId;
}
