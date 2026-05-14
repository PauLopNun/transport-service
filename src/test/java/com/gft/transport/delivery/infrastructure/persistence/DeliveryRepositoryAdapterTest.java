package com.gft.transport.delivery.infrastructure.persistence;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryRepositoryAdapterTest {

    @Mock
    private DeliveryJpaRepository jpaRepository;

    private DeliveryRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DeliveryRepositoryAdapter(jpaRepository);
    }

    @Test
    void savesDeliveryAsEntity() {
        Delivery delivery = delivery(new Location(1, 2), 5);

        adapter.save(delivery);

        ArgumentCaptor<DeliveryEntity> captor = ArgumentCaptor.forClass(DeliveryEntity.class);
        verify(jpaRepository).save(captor.capture());
        DeliveryEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(delivery.getDeliveryId().value());
        assertThat(saved.getShipmentId()).isEqualTo(delivery.getShipmentId());
        assertThat(saved.getTruckId()).isEqualTo(delivery.getTruckId().value());
        assertThat(saved.getOriginX()).isEqualTo(1);
        assertThat(saved.getOriginY()).isEqualTo(2);
        assertThat(saved.getDestX()).isEqualTo(9);
        assertThat(saved.getDestY()).isEqualTo(4);
        assertThat(saved.getAssignedAt()).isEqualTo(3);
        assertThat(saved.getCompletedAt()).isEqualTo(5);
        assertThat(saved.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getProductId()).isEqualTo("wood");
                    assertThat(item.getQuantity()).isEqualTo(2);
                });
    }

    @Test
    void savesNullOrigin() {
        Delivery delivery = delivery(null, null);

        adapter.save(delivery);

        ArgumentCaptor<DeliveryEntity> captor = ArgumentCaptor.forClass(DeliveryEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getOriginX()).isNull();
        assertThat(captor.getValue().getOriginY()).isNull();
    }

    @Test
    void findsDeliveryById() {
        DeliveryEntity entity = entity(new Location(1, 2), 8);
        when(jpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        Optional<Delivery> found = adapter.findById(new DeliveryId(entity.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().getDeliveryId().value()).isEqualTo(entity.getId());
        assertThat(found.get().getOrigin()).isEqualTo(new Location(1, 2));
        assertThat(found.get().getDestination()).isEqualTo(new Location(9, 4));
        assertThat(found.get().getCompletedAt()).isEqualTo(8);
        assertThat(found.get().getItems()).containsExactly(new DeliveryItem("wood", 2));
    }

    @Test
    void findsDeliveryWithNullOrigin() {
        DeliveryEntity entity = entity(null, null);
        when(jpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        Optional<Delivery> found = adapter.findById(new DeliveryId(entity.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().getOrigin()).isNull();
    }

    @Test
    void returnsEmptyWhenDeliveryIsMissing() {
        DeliveryId deliveryId = DeliveryId.generate();
        when(jpaRepository.findById(deliveryId.value())).thenReturn(Optional.empty());

        assertThat(adapter.findById(deliveryId)).isEmpty();
    }

    @Test
    void findsDeliveriesByTruckId() {
        TruckId truckId = TruckId.generate();
        DeliveryEntity first = entity(new Location(1, 2), null);
        DeliveryEntity second = entity(new Location(3, 4), 6);
        when(jpaRepository.findByTruckId(truckId.value())).thenReturn(List.of(first, second));

        assertThat(adapter.findByTruckId(truckId))
                .extracting(Delivery::getCompletedAt)
                .containsExactly(null, 6);
    }

    private Delivery delivery(Location origin, Integer completedAt) {
        return Delivery.builder()
                .deliveryId(DeliveryId.generate())
                .shipmentId(UUID.randomUUID())
                .truckId(TruckId.generate())
                .origin(origin)
                .destination(new Location(9, 4))
                .items(List.of(new DeliveryItem("wood", 2)))
                .assignedAt(3)
                .completedAt(completedAt)
                .build();
    }

    private DeliveryEntity entity(Location origin, Integer completedAt) {
        Delivery delivery = delivery(origin, completedAt);
        return DeliveryEntity.builder()
                .id(delivery.getDeliveryId().value())
                .shipmentId(delivery.getShipmentId())
                .truckId(delivery.getTruckId().value())
                .originX(origin == null ? null : origin.x())
                .originY(origin == null ? null : origin.y())
                .destX(delivery.getDestination().x())
                .destY(delivery.getDestination().y())
                .assignedAt(delivery.getAssignedAt())
                .completedAt(delivery.getCompletedAt())
                .items(List.of(new DeliveryItemEmbeddable("wood", 2)))
                .build();
    }
}
