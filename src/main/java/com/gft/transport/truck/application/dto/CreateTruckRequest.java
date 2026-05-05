package com.gft.transport.truck.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
@Schema(description = "Request body to register a new truck in the fleet")
public class CreateTruckRequest {
    @NotBlank
    @Schema(description = "Display name of the truck", example = "Truck 01")
    String name;

    @Schema(description = "Starting X coordinate on the grid", example = "0")
    int x;

    @Schema(description = "Starting Y coordinate on the grid", example = "0")
    int y;

    @Min(1)
    @Schema(description = "Maximum number of DeliveryItems the truck can carry", example = "10", minimum = "1")
    int capacity;
}
