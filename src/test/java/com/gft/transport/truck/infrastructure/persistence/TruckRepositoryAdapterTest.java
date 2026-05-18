package com.gft.transport.truck.infrastructure.persistence;

import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TruckRepositoryAdapterTest {

    @Mock
    private TruckJpaRepository jpaRepository;

    private TruckRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TruckRepositoryAdapter(jpaRepository);
    }

    @Test
    void savesTruckAsEntity() {
        Truck truck = truck(TruckStatus.IN_TRANSIT, 4, List.of(DeliveryId.generate()));

        adapter.save(truck);

        ArgumentCaptor<TruckEntity> captor = ArgumentCaptor.forClass(TruckEntity.class);
        verify(jpaRepository).save(captor.capture());
        TruckEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(truck.getTruckId().value());
        assertThat(saved.getName()).isEqualTo("Truck-1");
        assertThat(saved.getX()).isEqualTo(3);
        assertThat(saved.getY()).isEqualTo(7);
        assertThat(saved.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
        assertThat(saved.getCapacity()).isEqualTo(10);
        assertThat(saved.getCurrentLoad()).isEqualTo(4);
        assertThat(saved.getDeliveryIds()).containsExactly(truck.getDeliveryIds().get(0).value());
    }

    @Test
    void findsTruckById() {
        TruckEntity entity = entity(TruckStatus.AVAILABLE, 0, List.of(DeliveryId.generate()));
        when(jpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        Optional<Truck> found = adapter.findById(new TruckId(entity.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().getTruckId().value()).isEqualTo(entity.getId());
        assertThat(found.get().getLocation()).isEqualTo(new Location(3, 7));
        assertThat(found.get().getStatus()).isEqualTo(TruckStatus.AVAILABLE);
        assertThat(found.get().getDeliveryIds()).containsExactly(new DeliveryId(entity.getDeliveryIds().get(0)));
    }

    @Test
    void returnsEmptyWhenTruckIsMissing() {
        TruckId truckId = TruckId.generate();
        when(jpaRepository.findById(truckId.value())).thenReturn(Optional.empty());

        assertThat(adapter.findById(truckId)).isEmpty();
    }

    @Test
    void findsAllTrucks() {
        when(jpaRepository.findAll()).thenReturn(List.of(
                entity(TruckStatus.AVAILABLE, 0, List.of()),
                entity(TruckStatus.IN_TRANSIT, 5, List.of())
        ));

        assertThat(adapter.findAll())
                .extracting(Truck::getStatus)
                .containsExactly(TruckStatus.AVAILABLE, TruckStatus.IN_TRANSIT);
    }

    @Test
    void findsAvailableTrucks() {
        TruckEntity available = entity(TruckStatus.AVAILABLE, 0, List.of());
        when(jpaRepository.findByStatus(TruckStatus.AVAILABLE)).thenReturn(List.of(available));

        assertThat(adapter.findAvailable())
                .singleElement()
                .extracting(Truck::getStatus)
                .isEqualTo(TruckStatus.AVAILABLE);
    }

    @Test
    void deletesByTruckId() {
        TruckId truckId = TruckId.generate();

        adapter.deleteById(truckId);

        verify(jpaRepository).deleteById(truckId.value());
    }

    @Test
    void mapsOutgoingPendingDeletionToEntity() {
        Truck truck = truck(TruckStatus.IN_TRANSIT, 4, List.of(), true);

        adapter.save(truck);

        ArgumentCaptor<TruckEntity> captor = ArgumentCaptor.forClass(TruckEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().isPendingDeletion()).isTrue();
    }

    @Test
    void mapsIncomingPendingDeletionFromEntity() {
        TruckEntity entity = entity(TruckStatus.IN_TRANSIT, 4, List.of(), true);
        when(jpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        Truck found = adapter.findById(new TruckId(entity.getId())).orElseThrow();

        assertThat(found.isPendingDeletion()).isTrue();
    }

    private Truck truck(TruckStatus status, int load, List<DeliveryId> deliveryIds) {
        return truck(status, load, deliveryIds, false);
    }

    private Truck truck(TruckStatus status, int load, List<DeliveryId> deliveryIds, boolean pendingDeletion) {
        return Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck-1")
                .location(new Location(3, 7))
                .status(status)
                .capacity(10)
                .currentLoad(load)
                .deliveryIds(deliveryIds)
                .pendingDeletion(pendingDeletion)
                .build();
    }

    private TruckEntity entity(TruckStatus status, int load, List<DeliveryId> deliveryIds) {
        return entity(status, load, deliveryIds, false);
    }

    private TruckEntity entity(TruckStatus status, int load, List<DeliveryId> deliveryIds, boolean pendingDeletion) {
        Truck truck = truck(status, load, deliveryIds, pendingDeletion);
        return TruckEntity.builder()
                .id(truck.getTruckId().value())
                .name(truck.getName())
                .x(truck.getLocation().x())
                .y(truck.getLocation().y())
                .status(truck.getStatus())
                .capacity(truck.getCapacity())
                .currentLoad(truck.getCurrentLoad())
                .deliveryIds(deliveryIds.stream().map(DeliveryId::value).toList())
                .pendingDeletion(pendingDeletion)
                .build();
    }
}
