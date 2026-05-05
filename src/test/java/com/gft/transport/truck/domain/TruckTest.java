package com.gft.transport.truck.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TruckTest {

    private Truck buildTruck(int capacity, int currentLoad) {
        return Truck.builder()
                .truckId(new TruckId(UUID.randomUUID()))
                .name("Truck-A")
                .location(new Location(0, 0))
                .status(TruckStatus.AVAILABLE)
                .capacity(capacity)
                .currentLoad(currentLoad)
                .deliveryIds(List.of())
                .build();
    }

    @Test
    void remainingCapacityIsCapacityMinusCurrentLoad() {
        Truck truck = buildTruck(100, 40);
        assertThat(truck.remainingCapacity()).isEqualTo(60);
    }

    @Test
    void remainingCapacityIsZeroWhenFull() {
        Truck truck = buildTruck(50, 50);
        assertThat(truck.remainingCapacity()).isEqualTo(0);
    }

    @Test
    void canAcceptReturnsTrueWhenEnoughCapacity() {
        Truck truck = buildTruck(100, 30);
        assertThat(truck.canAccept(70)).isTrue();
    }

    @Test
    void canAcceptReturnsTrueWhenExactlyFit() {
        Truck truck = buildTruck(100, 30);
        assertThat(truck.canAccept(70)).isTrue();
    }

    @Test
    void canAcceptReturnsFalseWhenExceedsCapacity() {
        Truck truck = buildTruck(100, 30);
        assertThat(truck.canAccept(71)).isFalse();
    }

    @Test
    void canAcceptReturnsFalseWhenFull() {
        Truck truck = buildTruck(50, 50);
        assertThat(truck.canAccept(1)).isFalse();
    }

    @Test
    void canAcceptReturnsTrueForZeroItems() {
        Truck truck = buildTruck(50, 50);
        assertThat(truck.canAccept(0)).isTrue();
    }

    @Test
    void speedDefaultsToOne() {
        Truck truck = buildTruck(10, 0);
        assertThat(truck.getSpeed()).isEqualTo(1);
    }

    @Test
    void speedCanBeOverridden() {
        Truck truck = Truck.builder()
                .truckId(new TruckId(UUID.randomUUID()))
                .name("Fast")
                .location(new Location(0, 0))
                .status(TruckStatus.AVAILABLE)
                .capacity(10)
                .currentLoad(0)
                .speed(2)
                .deliveryIds(List.of())
                .build();
        assertThat(truck.getSpeed()).isEqualTo(2);
    }
}
