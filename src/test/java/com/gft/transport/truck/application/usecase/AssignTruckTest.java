package com.gft.transport.truck.application.usecase;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;
import com.gft.transport.truck.domain.repository.TruckRepository;
import com.gft.transport.truck.domain.service.DistanceCalculator;
import com.gft.transport.truck.domain.service.OptimalTruckSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignTruckTest {

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private TruckEventPublisher eventPublisher;

    private AssignTruck assignTruck;

    @BeforeEach
    void setUp() {
        assignTruck = new AssignTruck(
                truckRepository,
                deliveryRepository,
                new OptimalTruckSelector(new DistanceCalculator()),
                eventPublisher,
                new DistanceCalculator()
        );
    }

    @Test
    void assignsClosestAvailableTruckToShipment() {
        Truck truck = availableTruck(new Location(1, 0));
        when(truckRepository.findAll()).thenReturn(List.of(truck));

        assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 3));

        verify(truckRepository).save(any(Truck.class));
    }

    @Test
    void savesDeliveryOnAssignment() {
        when(truckRepository.findAll()).thenReturn(List.of(availableTruck(new Location(0, 0))));

        assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 2));

        verify(deliveryRepository).save(any());
    }

    @Test
    void publishesStatusChangedEventWithDispatchedReason() {
        when(truckRepository.findAll()).thenReturn(List.of(availableTruck(new Location(0, 0))));

        assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 2));

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        TruckStatusChangedEvent event = captor.getValue();
        assertThat(event.getOldStatus()).isEqualTo(TruckStatus.AVAILABLE);
        assertThat(event.getNewStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
        assertThat(event.getReason()).isEqualTo("DISPATCHED");
        assertThat(event.getTimestamp()).isEqualTo(1);
    }

    @Test
    void throwsWhenNoTruckAvailable() {
        when(truckRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 1)))
                .isInstanceOf(NoTruckAvailableException.class);
    }

    @Test
    void updatesCurrentLoadOnTruck() {
        when(truckRepository.findAll()).thenReturn(List.of(availableTruck(new Location(0, 0))));

        assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 3));

        ArgumentCaptor<Truck> captor = ArgumentCaptor.forClass(Truck.class);
        verify(truckRepository).save(captor.capture());
        Truck updated = captor.getValue();

        assertThat(updated.getCurrentLoad()).isEqualTo(3);
        assertThat(updated.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
    }

    @Test
    void assignsToInTransitTruckWhenNoAvailableTruck() {
        TruckId truckId = TruckId.generate();
        Truck inTransit = Truck.builder()
                .truckId(truckId)
                .name("Truck 01")
                .location(new Location(0, 0))
                .status(TruckStatus.IN_TRANSIT)
                .capacity(10)
                .currentLoad(3)
                .deliveryIds(List.of(com.gft.transport.delivery.domain.DeliveryId.generate()))
                .build();

        when(truckRepository.findAll()).thenReturn(List.of(inTransit));
        when(deliveryRepository.findByTruckId(truckId))
                .thenReturn(List.of(activeDelivery(truckId, new Location(5, 0))));

        assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 2));

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("LOAD_UPDATED");
        assertThat(captor.getValue().getOldStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
    }

    @Test
    void assignsToInTransitTruckAndFiltersOutAvailableWithNoCapacity() {
        Truck availableButFull = Truck.builder()
                .truckId(TruckId.generate()).name("AvailableFull")
                .location(new Location(0, 0)).status(TruckStatus.AVAILABLE)
                .capacity(3).currentLoad(3).deliveryIds(List.of()).build();

        TruckId inTransitTruckId = TruckId.generate();
        Truck inTransitWithCapacity = Truck.builder()
                .truckId(inTransitTruckId).name("InTransit")
                .location(new Location(1, 0)).status(TruckStatus.IN_TRANSIT)
                .capacity(10).currentLoad(2)
                .deliveryIds(List.of(com.gft.transport.delivery.domain.DeliveryId.generate())).build();

        when(truckRepository.findAll()).thenReturn(List.of(availableButFull, inTransitWithCapacity));
        when(deliveryRepository.findByTruckId(inTransitTruckId))
                .thenReturn(List.of(activeDelivery(inTransitTruckId, new Location(0, 5))));

        assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 5));

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("LOAD_UPDATED");
        assertThat(captor.getValue().getTruckId()).isEqualTo(inTransitWithCapacity.getTruckId());
    }

    @Test
    void throwsWhenInTransitTruckAlsoHasNoCapacity() {
        Truck full = Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck 01")
                .location(new Location(0, 0))
                .status(TruckStatus.IN_TRANSIT)
                .capacity(5)
                .currentLoad(5)
                .deliveryIds(List.of(com.gft.transport.delivery.domain.DeliveryId.generate()))
                .build();

        when(truckRepository.findAll()).thenReturn(List.of(full));

        assertThatThrownBy(() -> assignTruck.execute(command(new Location(0, 0), new Location(5, 5), 1)))
                .isInstanceOf(com.gft.transport.truck.domain.exception.NoTruckAvailableException.class);
    }

    @Test
    void assignsInTransitTruckWhoseRoutePassesThroughShipmentOrigin() {
        TruckId truckId = TruckId.generate();
        Truck inTransitTruck = inTransitTruck(truckId, new Location(2, 0));
        lenient().when(deliveryRepository.findByTruckId(truckId))
                .thenReturn(List.of(activeDelivery(truckId, new Location(5, 0))));

        when(truckRepository.findAll()).thenReturn(List.of(inTransitTruck));

        assignTruck.execute(command(new Location(3, 0), new Location(8, 8), 2));

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("LOAD_UPDATED");
    }

    @Test
    void doesNotAssignInTransitTruckWhenAllItsDeliveriesAreCompleted() {
        TruckId truckId = TruckId.generate();
        Truck inTransitTruck = inTransitTruck(truckId, new Location(2, 0));
        Delivery completedDelivery = activeDelivery(truckId, new Location(5, 0)).complete(1);

        when(truckRepository.findAll()).thenReturn(List.of(inTransitTruck));
        when(deliveryRepository.findByTruckId(truckId)).thenReturn(List.of(completedDelivery));

        assertThatThrownBy(() -> assignTruck.execute(command(new Location(3, 0), new Location(8, 8), 2)))
                .isInstanceOf(NoTruckAvailableException.class);
    }

    @Test
    void doesNotAssignInTransitTruckWhoseRouteDoesNotPassThroughShipmentOrigin() {
        TruckId truckId = TruckId.generate();
        Truck inTransitTruck = inTransitTruck(truckId, new Location(2, 0));

        when(truckRepository.findAll()).thenReturn(List.of(inTransitTruck));
        when(deliveryRepository.findByTruckId(truckId))
                .thenReturn(List.of(activeDelivery(truckId, new Location(5, 0))));

        assertThatThrownBy(() -> assignTruck.execute(command(new Location(3, 3), new Location(8, 8), 2)))
                .isInstanceOf(NoTruckAvailableException.class);
    }

    private Truck inTransitTruck(TruckId truckId, Location location) {
        return Truck.builder()
                .truckId(truckId)
                .name("Truck IN_TRANSIT")
                .location(location)
                .status(TruckStatus.IN_TRANSIT)
                .capacity(10)
                .currentLoad(3)
                .deliveryIds(List.of(DeliveryId.generate()))
                .build();
    }

    private Delivery activeDelivery(TruckId truckId, Location destination) {
        return Delivery.builder()
                .deliveryId(DeliveryId.generate())
                .shipmentId(UUID.randomUUID())
                .truckId(truckId)
                .origin(new Location(0, 0))
                .destination(destination)
                .items(List.of())
                .assignedAt(0)
                .completedAt(null)
                .build();
    }

    private Truck availableTruck(Location location) {
        return Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck 01")
                .location(location)
                .status(TruckStatus.AVAILABLE)
                .capacity(10)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();
    }

    private AssignTruckCommand command(Location origin, Location destination, int quantity) {
        return new AssignTruckCommand(
                UUID.randomUUID(),
                origin,
                destination,
                List.of(new DeliveryItem(UUID.randomUUID().toString(), quantity)),
                1
        );
    }
}
