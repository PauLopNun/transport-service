package com.gft.transport.delivery.infrastructure.persistence;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository jpaRepository;

    @Override
    public void save(Delivery delivery) {
        jpaRepository.save(toEntity(delivery));
    }

    @Override
    public Optional<Delivery> findById(DeliveryId deliveryId) {
        return jpaRepository.findById(deliveryId.value()).map(this::toDomain);
    }

    @Override
    public List<Delivery> findByTruckId(TruckId truckId) {
        return jpaRepository.findByTruckId(truckId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private DeliveryEntity toEntity(Delivery delivery) {
        List<DeliveryItemEmbeddable> items = delivery.getItems().stream()
                .map(i -> new DeliveryItemEmbeddable(i.materialType(), i.quantity()))
                .toList();

        Location origin = delivery.getOrigin();
        return DeliveryEntity.builder()
                .id(delivery.getDeliveryId().value())
                .shipmentId(delivery.getShipmentId())
                .truckId(delivery.getTruckId().value())
                .originX(origin != null ? origin.x() : null)
                .originY(origin != null ? origin.y() : null)
                .destX(delivery.getDestination().x())
                .destY(delivery.getDestination().y())
                .assignedAt(delivery.getAssignedAt())
                .completedAt(delivery.getCompletedAt())
                .items(items)
                .build();
    }

    private Delivery toDomain(DeliveryEntity entity) {
        List<DeliveryItem> items = entity.getItems().stream()
                .map(i -> new DeliveryItem(i.getMaterialType(), i.getQuantity()))
                .toList();

        Location origin = entity.getOriginX() != null
                ? new Location(entity.getOriginX(), entity.getOriginY())
                : null;
        return Delivery.builder()
                .deliveryId(new DeliveryId(entity.getId()))
                .shipmentId(entity.getShipmentId())
                .truckId(new TruckId(entity.getTruckId()))
                .origin(origin)
                .destination(new Location(entity.getDestX(), entity.getDestY()))
                .items(items)
                .assignedAt(entity.getAssignedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}