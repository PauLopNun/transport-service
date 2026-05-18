package com.gft.transport.truck.infrastructure.persistence;

import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TruckRepositoryAdapter implements TruckRepository {

    private final TruckJpaRepository jpaRepository;

    @Override
    @Transactional
    public void save(Truck truck) {
        jpaRepository.save(toEntity(truck));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Truck> findById(TruckId truckId) {
        return jpaRepository.findById(truckId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Truck> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Truck> findAvailable() {
        return jpaRepository.findByStatus(TruckStatus.AVAILABLE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(TruckId truckId) {
        jpaRepository.deleteById(truckId.value());
    }

    private TruckEntity toEntity(Truck truck) {
        return TruckEntity.builder()
                .id(truck.getTruckId().value())
                .name(truck.getName())
                .x(truck.getLocation().x())
                .y(truck.getLocation().y())
                .status(truck.getStatus())
                .capacity(truck.getCapacity())
                .currentLoad(truck.getCurrentLoad())
                .deliveryIds(truck.getDeliveryIds().stream()
                        .map(DeliveryId::value)
                        .toList())
                .pendingDeletion(truck.isPendingDeletion())
                .build();
    }

    private Truck toDomain(TruckEntity entity) {
        return Truck.builder()
                .truckId(new TruckId(entity.getId()))
                .name(entity.getName())
                .location(new Location(entity.getX(), entity.getY()))
                .status(entity.getStatus())
                .capacity(entity.getCapacity())
                .currentLoad(entity.getCurrentLoad())
                .deliveryIds(entity.getDeliveryIds().stream()
                        .map(DeliveryId::new)
                        .toList())
                .pendingDeletion(entity.isPendingDeletion())
                .build();
    }
}
