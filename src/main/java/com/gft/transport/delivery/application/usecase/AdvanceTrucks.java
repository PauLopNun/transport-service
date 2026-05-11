package com.gft.transport.delivery.application.usecase;

import com.gft.transport.delivery.application.port.out.DeliveryEventPublisher;
import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvanceTrucks {

    private final TruckRepository truckRepository;
    private final DeliveryRepository deliveryRepository;
    private final TruckEventPublisher truckEventPublisher;
    private final DeliveryEventPublisher deliveryEventPublisher;

    public void execute(int daysAdvanced, int currentDay) {
        truckRepository.findAll().stream()
                .filter(truck -> truck.getStatus() == TruckStatus.IN_TRANSIT)
                .forEach(truck -> advanceTruck(truck, daysAdvanced, currentDay));
    }

    private void advanceTruck(Truck truck, int daysAdvanced, int currentDay) {
        List<Delivery> pendingDeliveries = findPendingDeliveries(truck);
        if (pendingDeliveries.isEmpty()) return;

        Truck current = truck;
        List<Delivery> remaining = new ArrayList<>(pendingDeliveries);

        for (int day = 0; day < daysAdvanced && !remaining.isEmpty(); day++) {
            current = moveOneDayToward(current, remaining.get(0).getDestination());
            if (remaining.get(0).isArrived(current.getLocation())) {
                current = handleDeliveryArrival(current, remaining, currentDay);
            }
        }
    }

    private List<Delivery> findPendingDeliveries(Truck truck) {
        return deliveryRepository.findByTruckId(truck.getTruckId()).stream()
                .filter(delivery -> !delivery.isCompleted())
                .toList();
    }

    private Truck moveOneDayToward(Truck truck, Location destination) {
        Location nextPosition = calculateNextStep(truck.getLocation(), destination);
        Truck movedTruck = truck.toBuilder().location(nextPosition).build();
        truckRepository.save(movedTruck);
        truckEventPublisher.publish(new TruckPositionUpdatedEvent(movedTruck.getTruckId(), nextPosition));
        return movedTruck;
    }

    private Truck handleDeliveryArrival(Truck truck, List<Delivery> remainingDeliveries, int currentDay) {
        Delivery arrivedDelivery = remainingDeliveries.remove(0);
        completeDelivery(arrivedDelivery, currentDay);

        if (remainingDeliveries.isEmpty()) {
            return returnTruckToBase(truck, currentDay);
        }
        return unloadDeliveredItemsFromTruck(truck, arrivedDelivery, currentDay);
    }

    private void completeDelivery(Delivery delivery, int currentDay) {
        Delivery completedDelivery = delivery.complete(currentDay);
        deliveryRepository.save(completedDelivery);
        deliveryEventPublisher.publish(new DeliveryCompletedEvent(
                completedDelivery.getShipmentId(),
                completedDelivery.getTruckId(),
                completedDelivery.getItems(),
                completedDelivery.getDestination(),
                currentDay
        ));
    }

    private Truck returnTruckToBase(Truck truck, int currentDay) {
        Truck availableTruck = truck.toBuilder()
                .status(TruckStatus.AVAILABLE)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();
        truckRepository.save(availableTruck);
        truckEventPublisher.publish(new TruckStatusChangedEvent(
                availableTruck.getTruckId(),
                TruckStatus.IN_TRANSIT,
                TruckStatus.AVAILABLE,
                availableTruck.getLocation(),
                0,
                availableTruck.getCapacity(),
                currentDay,
                "RETURNED_TO_BASE"
        ));
        return availableTruck;
    }

    private Truck unloadDeliveredItemsFromTruck(Truck truck, Delivery completedDelivery, int currentDay) {
        int unloadedItemCount = completedDelivery.getItems().stream()
                .mapToInt(DeliveryItem::quantity)
                .sum();
        int updatedLoad = truck.getCurrentLoad() - unloadedItemCount;
        List<DeliveryId> remainingDeliveryIds = truck.getDeliveryIds().stream()
                .filter(id -> !id.equals(completedDelivery.getDeliveryId()))
                .toList();

        Truck updatedTruck = truck.toBuilder()
                .currentLoad(updatedLoad)
                .deliveryIds(remainingDeliveryIds)
                .build();
        truckRepository.save(updatedTruck);
        truckEventPublisher.publish(new TruckStatusChangedEvent(
                updatedTruck.getTruckId(),
                TruckStatus.IN_TRANSIT,
                TruckStatus.IN_TRANSIT,
                updatedTruck.getLocation(),
                updatedLoad,
                updatedTruck.getCapacity(),
                currentDay,
                "LOAD_UPDATED"
        ));
        return updatedTruck;
    }

    private Location calculateNextStep(Location current, Location destination) {
        if (current.x() != destination.x()) {
            int xDirection = destination.x() > current.x() ? 1 : -1;
            return new Location(current.x() + xDirection, current.y());
        }
        int yDirection = destination.y() > current.y() ? 1 : -1;
        return new Location(current.x(), current.y() + yDirection);
    }
}
