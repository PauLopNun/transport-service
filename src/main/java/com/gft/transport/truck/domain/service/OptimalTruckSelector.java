package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;

import java.util.List;

public class OptimalTruckSelector {

    private final DistanceCalculator distanceCalculator;

    public OptimalTruckSelector(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    public Truck select(List<Truck> trucks, Location origin, int requiredItems) {
        return trucks.stream()
                .filter(t -> t.getStatus() == TruckStatus.AVAILABLE)
                .filter(t -> t.canAccept(requiredItems))
                .min((a, b) -> Integer.compare(
                        distanceCalculator.calculate(a.getLocation(), origin),
                        distanceCalculator.calculate(b.getLocation(), origin)
                ))
                .orElseThrow(NoTruckAvailableException::new);
    }
}