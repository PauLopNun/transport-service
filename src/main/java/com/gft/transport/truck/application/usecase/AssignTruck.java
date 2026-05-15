package com.gft.transport.truck.application.usecase;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;
import com.gft.transport.truck.domain.repository.TruckRepository;
import com.gft.transport.truck.domain.service.DistanceCalculator;
import com.gft.transport.truck.domain.service.OptimalTruckSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
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
            selectedTruck = findInTransitTruckWithCapacity(allTrucks, totalItemCount, command.origin());
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
        log.info("Truck assigned: truckId={} shipmentId={} reason={} load={}/{}",
                dispatchedTruck.getTruckId().value(), command.shipmentId(), statusChangeReason,
                dispatchedTruck.getCurrentLoad(), dispatchedTruck.getCapacity());

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

    private Truck findInTransitTruckWithCapacity(List<Truck> trucks, int requiredItemCount, Location shipmentOrigin) {
        return trucks.stream()
                .filter(truck -> truck.getStatus() == TruckStatus.IN_TRANSIT)
                .filter(truck -> truck.canAccept(requiredItemCount))
                .filter(truck -> isTruckRoutePassingThrough(truck, shipmentOrigin))
                .min(Comparator.comparingInt(truck -> distanceCalculator.calculate(truck.getLocation(), shipmentOrigin)))
                .orElseThrow(NoTruckAvailableException::new);
    }

    private boolean isTruckRoutePassingThrough(Truck truck, Location shipmentOrigin) {
        return deliveryRepository.findByTruckId(truck.getTruckId()).stream()
                .filter(delivery -> !delivery.isCompleted())
                .anyMatch(delivery -> distanceCalculator.isOnRoute(shipmentOrigin, truck.getLocation(), delivery.getDestination()));
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
