package com.gft.transport.truck.application.usecase;

import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.domain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTrucks {

    private final TruckRepository truckRepository;

    public List<TruckResponse> execute() {
        return truckRepository.findAll().stream()
                .map(TruckResponse::from)
                .toList();
    }
}
