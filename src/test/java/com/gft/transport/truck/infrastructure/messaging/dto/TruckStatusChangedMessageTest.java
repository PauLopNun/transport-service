package com.gft.transport.truck.infrastructure.messaging.dto;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TruckStatusChangedMessageTest {

    @Test
    void mapsAllFieldsFromEvent() {
        TruckId truckId = TruckId.generate();
        TruckStatusChangedEvent event = new TruckStatusChangedEvent(
                truckId,
                TruckStatus.AVAILABLE,
                TruckStatus.IN_TRANSIT,
                new Location(3, 7),
                6,
                10,
                3,
                "DISPATCHED"
        );

        TruckStatusChangedMessage message = TruckStatusChangedMessage.from(event);

        assertThat(message.getTruckId()).isEqualTo(truckId.value().toString());
        assertThat(message.getOldStatus()).isEqualTo("AVAILABLE");
        assertThat(message.getNewStatus()).isEqualTo("IN_TRANSIT");
        assertThat(message.getPosition().getX()).isEqualTo(3);
        assertThat(message.getPosition().getY()).isEqualTo(7);
        assertThat(message.getCurrentLoad()).isEqualTo(6);
        assertThat(message.getCapacity()).isEqualTo(10);
        assertThat(message.getTimestamp()).isEqualTo(3);
        assertThat(message.getReason()).isEqualTo("DISPATCHED");
    }

    @Test
    void mapsOldStatusAsNullWhenTruckRegistered() {
        TruckStatusChangedEvent event = new TruckStatusChangedEvent(
                TruckId.generate(),
                null,
                TruckStatus.AVAILABLE,
                new Location(0, 0),
                0,
                10,
                0,
                "TRUCK_REGISTERED"
        );

        TruckStatusChangedMessage message = TruckStatusChangedMessage.from(event);

        assertThat(message.getOldStatus()).isNull();
        assertThat(message.getNewStatus()).isEqualTo("AVAILABLE");
        assertThat(message.getReason()).isEqualTo("TRUCK_REGISTERED");
    }
}
