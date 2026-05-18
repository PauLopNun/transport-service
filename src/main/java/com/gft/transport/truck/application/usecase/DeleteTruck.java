package com.gft.transport.truck.application.usecase;

import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckDeletedEvent;
import com.gft.transport.truck.domain.exception.TruckNotFoundException;
import com.gft.transport.truck.domain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteTruck {

    private final TruckRepository truckRepository;
    private final TruckEventPublisher truckEventPublisher;

    public DeleteTruckResult execute(TruckId truckId) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new TruckNotFoundException(truckId));

        if (truck.getStatus() == TruckStatus.AVAILABLE) {
            truckRepository.deleteById(truckId);
            truckEventPublisher.publish(new TruckDeletedEvent(truckId));
            return DeleteTruckResult.DELETED;
        }

        if (!truck.isPendingDeletion()) {
            truckRepository.save(truck.toBuilder().pendingDeletion(true).build());
        }
        return DeleteTruckResult.DELETION_SCHEDULED;
    }
}
