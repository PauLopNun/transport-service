package com.gft.transport.truck.infrastructure.persistence;

import com.gft.transport.truck.domain.TruckStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TruckJpaRepository extends JpaRepository<TruckEntity, UUID> {
    List<TruckEntity> findByStatus(TruckStatus status);
}
