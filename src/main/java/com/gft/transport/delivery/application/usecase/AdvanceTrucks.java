package com.gft.transport.delivery.application.usecase;

import com.gft.transport.delivery.application.port.out.DeliveryEventPublisher;
import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;
import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
                .filter(t -> t.getStatus() == TruckStatus.IN_TRANSIT)
                .forEach(truck -> processTruck(truck, daysAdvanced, currentDay));
    }

    private void processTruck(Truck truck, int daysAdvanced, int currentDay) {
        List<Delivery> pending = deliveryRepository.findByTruckId(truck.getTruckId()).stream()
                .filter(d -> !d.isCompleted())
                .toList();

        if (pending.isEmpty()) return;

        Truck current = truck;
        List<Delivery> remaining = new ArrayList<>(pending);

        for (int day = 0; day < daysAdvanced && !remaining.isEmpty(); day++) {
            Delivery target = remaining.get(0);
            Location next = nextStep(current.getLocation(), target.getDestination());

            current = rebuildTruck(current, next);
            truckRepository.save(current);
            truckEventPublisher.publish(new TruckPositionUpdatedEvent(current.getTruckId(), next));

            if (next.equals(target.getDestination())) {
                Delivery completed = rebuildDelivery(target, currentDay);
                deliveryRepository.save(completed);
                deliveryEventPublisher.publish(new DeliveryCompletedEvent(
                        completed.getShipmentId(), completed.getTruckId(),
                        completed.getItems(), completed.getDestination(),
                        currentDay, Instant.now()));

                remaining.remove(0);
                int freedItems = target.getItems().stream().mapToInt(i -> i.quantity()).sum();
                List<DeliveryId> updatedIds = new ArrayList<>(current.getDeliveryIds());
                updatedIds.remove(target.getDeliveryId());

                if (remaining.isEmpty()) {
                    current = rebuildTruckWithStatus(current, TruckStatus.AVAILABLE, 0, List.of());
                    truckRepository.save(current);
                    truckEventPublisher.publish(new TruckStatusChangedEvent(
                            current.getTruckId(), TruckStatus.IN_TRANSIT, TruckStatus.AVAILABLE,
                            current.getLocation(), 0, current.getCapacity(), Instant.now(), "RETURNED_TO_BASE"));
                } else {
                    int newLoad = current.getCurrentLoad() - freedItems;
                    current = rebuildTruckWithStatus(current, TruckStatus.IN_TRANSIT, newLoad, updatedIds);
                    truckRepository.save(current);
                    truckEventPublisher.publish(new TruckStatusChangedEvent(
                            current.getTruckId(), TruckStatus.IN_TRANSIT, TruckStatus.IN_TRANSIT,
                            current.getLocation(), newLoad, current.getCapacity(), Instant.now(), "LOAD_UPDATED"));
                }
            }
        }
    }

    private Location nextStep(Location current, Location destination) {
        if (current.x() != destination.x()) {
            return new Location(current.x() + (destination.x() > current.x() ? 1 : -1), current.y());
        }
        if (current.y() != destination.y()) {
            return new Location(current.x(), current.y() + (destination.y() > current.y() ? 1 : -1));
        }
        return current;
    }

    private Truck rebuildTruck(Truck truck, Location location) {
        return Truck.builder()
                .truckId(truck.getTruckId()).name(truck.getName()).location(location)
                .status(truck.getStatus()).capacity(truck.getCapacity())
                .currentLoad(truck.getCurrentLoad()).deliveryIds(truck.getDeliveryIds())
                .build();
    }

    private Truck rebuildTruckWithStatus(Truck truck, TruckStatus status, int load, List<DeliveryId> ids) {
        return Truck.builder()
                .truckId(truck.getTruckId()).name(truck.getName()).location(truck.getLocation())
                .status(status).capacity(truck.getCapacity()).currentLoad(load).deliveryIds(ids)
                .build();
    }

    private Delivery rebuildDelivery(Delivery delivery, int completedAt) {
        return Delivery.builder()
                .deliveryId(delivery.getDeliveryId()).shipmentId(delivery.getShipmentId())
                .truckId(delivery.getTruckId()).destination(delivery.getDestination())
                .items(delivery.getItems()).assignedAt(delivery.getAssignedAt())
                .completedAt(completedAt)
                .build();
    }
}