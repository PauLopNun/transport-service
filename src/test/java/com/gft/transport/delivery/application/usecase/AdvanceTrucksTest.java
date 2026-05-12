package com.gft.transport.delivery.application.usecase;

import com.gft.transport.delivery.application.port.out.DeliveryEventPublisher;
import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.repository.TruckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvanceTrucksTest {

    @Mock private TruckRepository truckRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private TruckEventPublisher truckEventPublisher;
    @Mock private DeliveryEventPublisher deliveryEventPublisher;

    private AdvanceTrucks advanceTrucks;

    @BeforeEach
    void setUp() {
        advanceTrucks = new AdvanceTrucks(truckRepository, deliveryRepository, truckEventPublisher, deliveryEventPublisher);
    }

    @Test
    void doesNothingWhenNoInTransitTrucks() {
        when(truckRepository.findAll()).thenReturn(List.of(availableTruck(new Location(0, 0))));

        advanceTrucks.execute(1, 2);

        verifyNoInteractions(truckEventPublisher, deliveryEventPublisher);
    }

    @Test
    void movesTruckOneStepTowardDestinationPerDay() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery delivery = deliveryFor(truck, new Location(3, 0));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(1, 2);

        ArgumentCaptor<Truck> captor = ArgumentCaptor.forClass(Truck.class);
        verify(truckRepository, atLeastOnce()).save(captor.capture());
        Truck saved = captor.getAllValues().get(0);
        assertThat(saved.getLocation()).isEqualTo(new Location(1, 0));
    }

    @Test
    void publishesPositionUpdateForEachStep() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery delivery = deliveryFor(truck, new Location(3, 0));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(2, 3);

        verify(truckEventPublisher, times(2)).publish(any(TruckPositionUpdatedEvent.class));
    }

    @Test
    void completesDeliveryWhenTruckArrivesAtDestination() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery delivery = deliveryFor(truck, new Location(1, 0));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(1, 2);

        verify(deliveryRepository).save(argThat(d -> d.getCompletedAt() != null));
        verify(deliveryEventPublisher).publish(any());
    }

    @Test
    void returnsTruckToAvailableWhenAllDeliveriesCompleted() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery delivery = deliveryFor(truck, new Location(1, 0));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(1, 2);

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(truckEventPublisher).publish(captor.capture());

        TruckStatusChangedEvent event = captor.getValue();
        assertThat(event.getNewStatus()).isEqualTo(TruckStatus.AVAILABLE);
        assertThat(event.getReason()).isEqualTo("RETURNED_TO_BASE");
        assertThat(event.getTimestamp()).isEqualTo(2);
    }

    @Test
    void keepsTruckInTransitAfterPartialDelivery() {
        TruckId truckId = TruckId.generate();
        DeliveryId firstId = DeliveryId.generate();
        DeliveryId secondId = DeliveryId.generate();

        Truck truck = Truck.builder()
                .truckId(truckId)
                .name("Truck")
                .location(new Location(0, 0))
                .status(TruckStatus.IN_TRANSIT)
                .capacity(10)
                .currentLoad(4)
                .deliveryIds(List.of(firstId, secondId))
                .build();

        Delivery first = Delivery.builder()
                .deliveryId(firstId)
                .shipmentId(UUID.randomUUID())
                .truckId(truckId)
                .origin(new Location(0, 0))
                .destination(new Location(1, 0))
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 2)))
                .assignedAt(1)
                .completedAt(null)
                .build();

        Delivery second = Delivery.builder()
                .deliveryId(secondId)
                .shipmentId(UUID.randomUUID())
                .truckId(truckId)
                .origin(new Location(0, 0))
                .destination(new Location(3, 0))
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 2)))
                .assignedAt(1)
                .completedAt(null)
                .build();

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truckId)).thenReturn(List.of(first, second));

        advanceTrucks.execute(1, 2);

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(truckEventPublisher).publish(captor.capture());

        TruckStatusChangedEvent event = captor.getValue();
        assertThat(event.getNewStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
        assertThat(event.getReason()).isEqualTo("LOAD_UPDATED");
    }

    @Test
    void publishesPositionUpdateWithCurrentLocation() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery delivery = deliveryFor(truck, new Location(3, 0));
        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(1, 2);

        ArgumentCaptor<TruckPositionUpdatedEvent> captor = ArgumentCaptor.forClass(TruckPositionUpdatedEvent.class);
        verify(truckEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getLocation()).isEqualTo(new Location(1, 0));
    }

    @Test
    void movesTruckOnYAxisAfterReachingDestinationX() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery delivery = deliveryFor(truck, new Location(0, 2));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(1, 2);

        ArgumentCaptor<Truck> captor = ArgumentCaptor.forClass(Truck.class);
        verify(truckRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getLocation()).isEqualTo(new Location(0, 1));
    }

    @Test
    void movesTruckInNegativeXDirectionWhenDestinationIsToTheLeft() {
        Truck truck = inTransitTruck(new Location(3, 0));
        Delivery delivery = deliveryFor(truck, new Location(0, 0));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(1, 2);

        ArgumentCaptor<Truck> captor = ArgumentCaptor.forClass(Truck.class);
        verify(truckRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getLocation()).isEqualTo(new Location(2, 0));
    }

    @Test
    void movesTruckInNegativeYDirectionWhenDestinationIsBelow() {
        Truck truck = inTransitTruck(new Location(0, 3));
        Delivery delivery = deliveryFor(truck, new Location(0, 0));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(1, 2);

        ArgumentCaptor<Truck> captor = ArgumentCaptor.forClass(Truck.class);
        verify(truckRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getLocation()).isEqualTo(new Location(0, 2));
    }

    @Test
    void stopsAdvancingWhenAllDeliveriesCompleteBeforeAllDaysElapse() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery delivery = deliveryFor(truck, new Location(1, 0));

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(delivery));

        advanceTrucks.execute(3, 2);

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(truckEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getNewStatus()).isEqualTo(TruckStatus.AVAILABLE);
        assertThat(captor.getValue().getReason()).isEqualTo("RETURNED_TO_BASE");
    }

    @Test
    void ignoresAlreadyCompletedDeliveriesWhenAdvancing() {
        Truck truck = inTransitTruck(new Location(0, 0));
        Delivery alreadyCompleted = Delivery.builder()
                .deliveryId(truck.getDeliveryIds().get(0))
                .shipmentId(UUID.randomUUID())
                .truckId(truck.getTruckId())
                .origin(new Location(0, 0))
                .destination(new Location(3, 0))
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 3)))
                .assignedAt(1)
                .completedAt(1)
                .build();

        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findByTruckId(truck.getTruckId())).thenReturn(List.of(alreadyCompleted));

        advanceTrucks.execute(1, 2);

        verify(truckRepository, never()).save(any());
        verifyNoInteractions(truckEventPublisher, deliveryEventPublisher);
    }

    @Test
    void doesNotMoveInTransitTruckWithNoPendingDeliveries() {
        Truck truckWithAllDeliveriesCompleted = Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck")
                .location(new Location(0, 0))
                .status(TruckStatus.IN_TRANSIT)
                .capacity(10)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();

        when(truckRepository.findAll()).thenReturn(List.of(truckWithAllDeliveriesCompleted));
        when(deliveryRepository.findByTruckId(truckWithAllDeliveriesCompleted.getTruckId())).thenReturn(List.of());

        advanceTrucks.execute(1, 2);

        verify(truckRepository, never()).save(any());
        verifyNoInteractions(truckEventPublisher, deliveryEventPublisher);
    }

    private Truck inTransitTruck(Location location) {
        return Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck")
                .location(location)
                .status(TruckStatus.IN_TRANSIT)
                .capacity(10)
                .currentLoad(3)
                .deliveryIds(List.of(DeliveryId.generate()))
                .build();
    }

    private Truck availableTruck(Location location) {
        return Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck")
                .location(location)
                .status(TruckStatus.AVAILABLE)
                .capacity(10)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();
    }

    private Delivery deliveryFor(Truck truck, Location destination) {
        return Delivery.builder()
                .deliveryId(truck.getDeliveryIds().get(0))
                .shipmentId(UUID.randomUUID())
                .truckId(truck.getTruckId())
                .origin(new Location(0, 0))
                .destination(destination)
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 3)))
                .assignedAt(1)
                .completedAt(null)
                .build();
    }
}
