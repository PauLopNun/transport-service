package com.gft.transport.delivery.domain.repository;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.truck.domain.TruckId;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository {
    void save(Delivery delivery);
    Optional<Delivery> findById(DeliveryId deliveryId);
    List<Delivery> findByTruckId(TruckId truckId);
}
