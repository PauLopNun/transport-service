package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.exception.NoTruckAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptimalTruckSelectorTest {

    private OptimalTruckSelector selector;

    @BeforeEach
    void setUp() {
        selector = new OptimalTruckSelector(new DistanceCalculator());
    }

    @Test
    void selectsTheClosestAvailableTruck() {
        Truck near = truckAt(new Location(1, 0), 10, 0);
        Truck far = truckAt(new Location(5, 0), 10, 0);

        Truck selected = selector.select(List.of(near, far), new Location(0, 0), 1);

        assertThat(selected).isEqualTo(near);
    }

    @Test
    void throwsWhenNoTrucksAvailable() {
        assertThatThrownBy(() -> selector.select(List.of(), new Location(0, 0), 1))
                .isInstanceOf(NoTruckAvailableException.class);
    }

    @Test
    void ignoresTrucksWithInsufficientCapacity() {
        Truck full = truckAt(new Location(1, 0), 5, 5);
        Truck withCapacity = truckAt(new Location(3, 0), 10, 0);

        Truck selected = selector.select(List.of(full, withCapacity), new Location(0, 0), 3);

        assertThat(selected).isEqualTo(withCapacity);
    }

    @Test
    void throwsWhenNoTruckHasSufficientCapacity() {
        Truck full = truckAt(new Location(1, 0), 5, 5);

        assertThatThrownBy(() -> selector.select(List.of(full), new Location(0, 0), 3))
                .isInstanceOf(NoTruckAvailableException.class);
    }

    @Test
    void ignoresNonAvailableTrucks() {
        Truck inTransit = truckAt(new Location(1, 0), 10, 0, TruckStatus.IN_TRANSIT);
        Truck available = truckAt(new Location(5, 0), 10, 0, TruckStatus.AVAILABLE);

        Truck selected = selector.select(List.of(inTransit, available), new Location(0, 0), 1);

        assertThat(selected).isEqualTo(available);
    }

    @Test
    void whenTiedOnDistanceSelectsFirst() {
        Truck first = truckAt(new Location(2, 0), 10, 0);
        Truck second = truckAt(new Location(0, 2), 10, 0);

        Truck selected = selector.select(List.of(first, second), new Location(0, 0), 1);

        assertThat(selected).isEqualTo(first);
    }

    private Truck truckAt(Location location, int capacity, int currentLoad) {
        return truckAt(location, capacity, currentLoad, TruckStatus.AVAILABLE);
    }

    private Truck truckAt(Location location, int capacity, int currentLoad, TruckStatus status) {
        return Truck.builder()
                .truckId(new TruckId(UUID.randomUUID()))
                .name("Truck")
                .location(location)
                .status(status)
                .capacity(capacity)
                .currentLoad(currentLoad)
                .deliveryIds(List.of())
                .build();
    }
}
