package com.gft.transport.truck.application.usecase;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;
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

        List<Truck> allTrucks = truckRepository.findAll();
        boolean additionalLoad = false;
        Truck truck;

        try {
            truck = truckSelector.select(allTrucks, command.origin(), requiredItems);
        } catch (NoTruckAvailableException e) {
            truck = allTrucks.stream()
                    .filter(t -> t.getStatus() == TruckStatus.IN_TRANSIT)
                    .filter(t -> t.canAccept(requiredItems))
                    .findFirst()
                    .orElseThrow(NoTruckAvailableException::new);
            additionalLoad = true;
        }

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
                .speed(truck.getSpeed())
                .currentLoad(truck.getCurrentLoad() + requiredItems)
                .deliveryIds(updatedDeliveryIds)
                .build();

        truckRepository.save(updatedTruck);

        TruckStatus oldStatus = additionalLoad ? TruckStatus.IN_TRANSIT : TruckStatus.AVAILABLE;
        String reason = additionalLoad ? "LOAD_UPDATED" : "DISPATCHED";

        eventPublisher.publish(new TruckStatusChangedEvent(
                updatedTruck.getTruckId(),
                oldStatus,
                TruckStatus.IN_TRANSIT,
                updatedTruck.getLocation(),
                updatedTruck.getCurrentLoad(),
                updatedTruck.getCapacity(),
                Instant.now(),
                reason
        ));
    }
}
