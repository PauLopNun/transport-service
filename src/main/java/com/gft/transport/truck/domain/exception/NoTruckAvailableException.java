package com.gft.transport.truck.domain.exception;

public class NoTruckAvailableException extends RuntimeException {
    public NoTruckAvailableException() {
        super("No available truck found for the requested shipment");
    }
}
