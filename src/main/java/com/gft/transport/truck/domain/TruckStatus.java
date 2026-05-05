package com.gft.transport.truck.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Operational status of a truck in the fleet")
public enum TruckStatus {
    AVAILABLE,
    IN_TRANSIT,
    DELIVERED
}
