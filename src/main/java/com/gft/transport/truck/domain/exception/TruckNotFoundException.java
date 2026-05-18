package com.gft.transport.truck.domain.exception;

import com.gft.transport.truck.domain.TruckId;

public class TruckNotFoundException extends RuntimeException {
    public TruckNotFoundException(TruckId truckId) {
        super("Truck not found: " + truckId.value());
    }
}
