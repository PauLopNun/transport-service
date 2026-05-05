package com.gft.transport.truck.infrastructure.rest;

import com.gft.transport.truck.application.dto.CreateTruckRequest;
import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.application.usecase.GetTrucks;
import com.gft.transport.truck.application.usecase.RegisterTruck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trucks")
@RequiredArgsConstructor
@Tag(name = "Trucks", description = "Fleet management — register trucks and query current state")
public class TruckController {

    private final RegisterTruck registerTruck;
    private final GetTrucks getTrucks;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new truck", description = "Creates a truck in the fleet. Publishes truck.registered.v1 and truck.status.changed.v1 (reason: TRUCK_REGISTERED) to RabbitMQ.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Truck registered"),
            @ApiResponse(responseCode = "400", description = "Validation error — blank name or capacity < 1")
    })
    public TruckResponse register(@RequestBody @Valid CreateTruckRequest request) {
        return registerTruck.execute(request);
    }

    @GetMapping
    @Operation(summary = "Get all trucks", description = "Returns current fleet state. Used by Map (UI) on startup before receiving events.")
    @ApiResponse(responseCode = "200", description = "List of trucks with current location and status")
    public List<TruckResponse> findAll() {
        return getTrucks.execute();
    }
}
