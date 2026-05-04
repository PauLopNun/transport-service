package com.gft.transport.truck.domain.repository;

import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;

import java.util.List;
import java.util.Optional;

public interface TruckRepository {
    void save(Truck truck);
    Optional<Truck> findById(TruckId truckId);
    List<Truck> findAll();
    List<Truck> findAvailable();
}
