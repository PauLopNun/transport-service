package com.gft.transport.delivery.domain;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryTest {

    private Delivery buildDelivery(Integer completedAt) {
        return Delivery.builder()
                .deliveryId(new DeliveryId(UUID.randomUUID()))
                .shipmentId(UUID.randomUUID())
                .truckId(new TruckId(UUID.randomUUID()))
                .origin(new Location(0, 0))
                .destination(new Location(5, 10))
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 3)))
                .assignedAt(1)
                .completedAt(completedAt)
                .build();
    }

    @Test
    void isCompletedReturnsFalseWhenCompletedAtIsNull() {
        assertThat(buildDelivery(null).isCompleted()).isFalse();
    }

    @Test
    void isCompletedReturnsTrueWhenCompletedAtIsSet() {
        assertThat(buildDelivery(5).isCompleted()).isTrue();
    }

    @Test
    void isArrivedReturnsTrueWhenTruckIsAtDestination() {
        Delivery delivery = buildDelivery(null);
        assertThat(delivery.isArrived(new Location(5, 10))).isTrue();
    }

    @Test
    void isArrivedReturnsFalseWhenTruckIsNotAtDestination() {
        Delivery delivery = buildDelivery(null);
        assertThat(delivery.isArrived(new Location(3, 10))).isFalse();
    }

    @Test
    void completeReturnsDeliveryWithCompletedAt() {
        Delivery delivery = buildDelivery(null);
        Delivery completed = delivery.complete(7);
        assertThat(completed.getCompletedAt()).isEqualTo(7);
        assertThat(completed.isCompleted()).isTrue();
    }

    @Test
    void completePreservesOriginAndAllFields() {
        Delivery delivery = buildDelivery(null);
        Delivery completed = delivery.complete(3);
        assertThat(completed.getOrigin()).isEqualTo(delivery.getOrigin());
        assertThat(completed.getDestination()).isEqualTo(delivery.getDestination());
        assertThat(completed.getShipmentId()).isEqualTo(delivery.getShipmentId());
    }
}
