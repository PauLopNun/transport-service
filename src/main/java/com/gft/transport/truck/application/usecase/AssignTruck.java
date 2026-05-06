package com.gft.transport.truck.application.usecase;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.repository.TruckRepository;
import com.gft.transport.truck.domain.service.OptimalTruckSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignTruck {

    private final TruckRepository truckRepository;
    private final DeliveryRepository deliveryRepository;
    private final OptimalTruckSelector truckSelector;
    private final TruckEventPublisher eventPublisher;

    public void execute(AssignTruckCommand command) {
        int requiredItems = command.items().stream()
                .mapToInt(item -> item.quantity())
                .sum();

        Truck truck = truckSelector.select(truckRepository.findAll(), command.origin(), requiredItems);

        Delivery delivery = Delivery.builder()
                .deliveryId(DeliveryId.generate())
                .shipmentId(command.shipmentId())
                .truckId(truck.getTruckId())
                .destination(command.destination())
                .items(command.items())
                .assignedAt(command.requestedAt())
                .completedAt(null)
                .build();

        deliveryRepository.save(delivery);

        var updatedDeliveryIds = new ArrayList<>(truck.getDeliveryIds());
        updatedDeliveryIds.add(delivery.getDeliveryId());

        Truck updatedTruck = Truck.builder()
                .truckId(truck.getTruckId())
                .name(truck.getName())
                .location(truck.getLocation())
                .status(TruckStatus.IN_TRANSIT)
                .capacity(truck.getCapacity())
                .currentLoad(truck.getCurrentLoad() + requiredItems)
                .deliveryIds(updatedDeliveryIds)
                .build();

        truckRepository.save(updatedTruck);

        eventPublisher.publish(new TruckStatusChangedEvent(
                updatedTruck.getTruckId(),
                TruckStatus.AVAILABLE,
                TruckStatus.IN_TRANSIT,
                updatedTruck.getLocation(),
                updatedTruck.getCurrentLoad(),
                updatedTruck.getCapacity(),
                Instant.now(),
                "DISPATCHED"
        ));
    }
}
