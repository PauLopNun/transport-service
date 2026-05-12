package com.gft.transport.truck.application.usecase;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;
import com.gft.transport.truck.domain.repository.TruckRepository;
import com.gft.transport.truck.domain.service.DistanceCalculator;
import com.gft.transport.truck.domain.service.OptimalTruckSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignTruck {

    private final TruckRepository truckRepository;
    private final DeliveryRepository deliveryRepository;
    private final OptimalTruckSelector truckSelector;
    private final TruckEventPublisher eventPublisher;
    private final DistanceCalculator distanceCalculator;

    public void execute(AssignTruckCommand command) {
        int totalItemCount = command.items().stream()
                .mapToInt(DeliveryItem::quantity)
                .sum();

        List<Truck> allTrucks = truckRepository.findAll();
        boolean isAssigningToInTransitTruck = false;
        Truck selectedTruck;

        try {
            selectedTruck = truckSelector.select(allTrucks, command.origin(), totalItemCount);
        } catch (NoTruckAvailableException e) {
            selectedTruck = findInTransitTruckWithCapacity(allTrucks, totalItemCount);
            isAssigningToInTransitTruck = true;
        }

        Delivery newDelivery = buildDelivery(command, selectedTruck);
        deliveryRepository.save(newDelivery);

        List<DeliveryId> deliveryIdsWithNewAssignment = new ArrayList<>(selectedTruck.getDeliveryIds());
        deliveryIdsWithNewAssignment.add(newDelivery.getDeliveryId());

        Truck dispatchedTruck = selectedTruck.toBuilder()
                .status(TruckStatus.IN_TRANSIT)
                .currentLoad(selectedTruck.getCurrentLoad() + totalItemCount)
                .deliveryIds(deliveryIdsWithNewAssignment)
                .build();

        truckRepository.save(dispatchedTruck);

        TruckStatus previousStatus = isAssigningToInTransitTruck ? TruckStatus.IN_TRANSIT : TruckStatus.AVAILABLE;
        String statusChangeReason = isAssigningToInTransitTruck ? "LOAD_UPDATED" : "DISPATCHED";

        eventPublisher.publish(new TruckStatusChangedEvent(
                dispatchedTruck.getTruckId(),
                previousStatus,
                TruckStatus.IN_TRANSIT,
                dispatchedTruck.getLocation(),
                dispatchedTruck.getCurrentLoad(),
                dispatchedTruck.getCapacity(),
                command.requestedAt(),
                statusChangeReason
        ));
    }

    private Truck findInTransitTruckWithCapacity(List<Truck> trucks, int requiredItemCount) {
        return trucks.stream()
                .filter(truck -> truck.getStatus() == TruckStatus.IN_TRANSIT)
                .filter(truck -> truck.canAccept(requiredItemCount))
                .findFirst()
                .orElseThrow(NoTruckAvailableException::new);
    }

    private Delivery buildDelivery(AssignTruckCommand command, Truck truck) {
        return Delivery.builder()
                .deliveryId(DeliveryId.generate())
                .shipmentId(command.shipmentId())
                .truckId(truck.getTruckId())
                .origin(command.origin())
                .destination(command.destination())
                .items(command.items())
                .assignedAt(command.requestedAt())
                .completedAt(null)
                .build();
    }
}
