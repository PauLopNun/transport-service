package com.gft.transport.truck.infrastructure.rest;

import com.gft.transport.truck.application.dto.CreateTruckRequest;
import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.application.usecase.GetTrucks;
import com.gft.transport.truck.application.usecase.RegisterTruck;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trucks")
@RequiredArgsConstructor
public class TruckController {

    private final RegisterTruck registerTruck;
    private final GetTrucks getTrucks;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TruckResponse register(@RequestBody @Valid CreateTruckRequest request) {
        return registerTruck.execute(request);
    }

    @GetMapping
    public List<TruckResponse> findAll() {
        return getTrucks.execute();
    }
}
