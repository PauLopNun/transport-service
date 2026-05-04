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
                .destination(new Location(5, 10))
                .items(List.of(new DeliveryItem("PALLETS", 3)))
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
}
