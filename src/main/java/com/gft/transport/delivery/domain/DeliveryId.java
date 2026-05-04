package com.gft.transport.delivery.domain;

import java.util.UUID;

public record DeliveryId(UUID value) {
    public static DeliveryId generate() {
        return new DeliveryId(UUID.randomUUID());
    }
}
