package com.gft.transport.truck.domain;

import java.util.UUID;

public record TruckId(UUID value) {
    public static TruckId generate() {
        return new TruckId(UUID.randomUUID());
    }
}
