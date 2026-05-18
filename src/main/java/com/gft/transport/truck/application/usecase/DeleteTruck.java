package com.gft.transport.truck.application.usecase;

import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteTruck {

    private final TruckRepository truckRepository;

    public DeleteTruckResult execute(TruckId truckId) {
        throw new UnsupportedOperationException("not implemented");
    }
}
