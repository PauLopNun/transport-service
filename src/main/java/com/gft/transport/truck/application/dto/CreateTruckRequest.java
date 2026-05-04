package com.gft.transport.truck.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class CreateTruckRequest {
    @NotBlank
    String name;
    int x;
    int y;
    @Min(1)
    int capacity;
}
