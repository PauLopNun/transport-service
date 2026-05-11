package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;

import java.util.Comparator;
import java.util.List;

public class OptimalTruckSelector {

    private final DistanceCalculator distanceCalculator;

    public OptimalTruckSelector(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    public Truck select(List<Truck> trucks, Location origin, int requiredItemCount) {
        return trucks.stream()
                .filter(truck -> truck.getStatus() == TruckStatus.AVAILABLE)
                .filter(truck -> truck.canAccept(requiredItemCount))
                .min(Comparator.comparingInt(truck -> distanceCalculator.calculate(truck.getLocation(), origin)))
                .orElseThrow(NoTruckAvailableException::new);
    }
}
