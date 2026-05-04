package com.gft.transport.truck.application.usecase;

import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.repository.TruckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTrucksTest {

    @Mock
    private TruckRepository truckRepository;

    private GetTrucks getTrucks;

    @BeforeEach
    void setUp() {
        getTrucks = new GetTrucks(truckRepository);
    }

    @Test
    void returnsEmptyListWhenNoTrucks() {
        when(truckRepository.findAll()).thenReturn(List.of());

        assertThat(getTrucks.execute()).isEmpty();
    }

    @Test
    void returnsMappedTruckResponses() {
        Truck truck = Truck.builder()
                .truckId(new TruckId(UUID.randomUUID()))
                .name("Truck 01")
                .location(new Location(3, 5))
                .status(TruckStatus.AVAILABLE)
                .capacity(10)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();

        when(truckRepository.findAll()).thenReturn(List.of(truck));

        List<TruckResponse> result = getTrucks.execute();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Truck 01");
        assertThat(result.get(0).locationX()).isEqualTo(3);
        assertThat(result.get(0).locationY()).isEqualTo(5);
        assertThat(result.get(0).status()).isEqualTo(TruckStatus.AVAILABLE);
    }

    @Test
    void returnsAllTrucks() {
        List<Truck> trucks = List.of(
                buildTruck("Truck 01"),
                buildTruck("Truck 02"),
                buildTruck("Truck 03")
        );

        when(truckRepository.findAll()).thenReturn(trucks);

        assertThat(getTrucks.execute()).hasSize(3);
    }

    private Truck buildTruck(String name) {
        return Truck.builder()
                .truckId(new TruckId(UUID.randomUUID()))
                .name(name)
                .location(new Location(0, 0))
                .status(TruckStatus.AVAILABLE)
                .capacity(10)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();
    }
}
