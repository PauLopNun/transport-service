package com.gft.transport.truck.application.usecase;

import com.gft.transport.truck.application.dto.CreateTruckRequest;
import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterTruck {

    private static final int SIMULATION_START_DAY = 0;

    private final TruckRepository truckRepository;
    private final TruckEventPublisher eventPublisher;

    public TruckResponse execute(CreateTruckRequest request) {
        Truck truck = Truck.builder()
                .truckId(TruckId.generate())
                .name(request.getName())
                .location(new Location(request.getX(), request.getY()))
                .status(TruckStatus.AVAILABLE)
                .capacity(request.getCapacity())
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();

        truckRepository.save(truck);

        eventPublisher.publish(new TruckRegisteredEvent(
                truck.getTruckId(),
                truck.getName(),
                truck.getLocation(),
                truck.getCapacity(),
                SIMULATION_START_DAY
        ));

        eventPublisher.publish(new TruckStatusChangedEvent(
                truck.getTruckId(),
                null,
                TruckStatus.AVAILABLE,
                truck.getLocation(),
                0,
                truck.getCapacity(),
                SIMULATION_START_DAY,
                "TRUCK_REGISTERED"
        ));

        return TruckResponse.from(truck);
    }
}
