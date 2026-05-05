package com.gft.transport.delivery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryJpaRepository extends JpaRepository<DeliveryEntity, UUID> {
    List<DeliveryEntity> findByTruckId(UUID truckId);
}