package com.gft.transport.truck.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarehouseRegisteredMessage(
        String warehouseId,
        LocationDto location
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationDto(int x, int y) {}
}
