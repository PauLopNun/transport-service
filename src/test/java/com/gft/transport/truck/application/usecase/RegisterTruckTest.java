package com.gft.transport.truck.application.usecase;

import com.gft.transport.truck.application.dto.CreateTruckRequest;
import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.repository.TruckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterTruckTest {

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private TruckEventPublisher eventPublisher;

    private RegisterTruck registerTruck;

    @BeforeEach
    void setUp() {
        registerTruck = new RegisterTruck(truckRepository, eventPublisher);
    }

    @Test
    void savesTruckToRepository() {
        registerTruck.execute(new CreateTruckRequest("Truck 01", 0, 0, 10));

        verify(truckRepository).save(any(Truck.class));
    }

    @Test
    void returnsTruckResponseWithCorrectData() {
        TruckResponse response = registerTruck.execute(new CreateTruckRequest("Truck 01", 3, 5, 10));

        assertThat(response.name()).isEqualTo("Truck 01");
        assertThat(response.location().x()).isEqualTo(3);
        assertThat(response.location().y()).isEqualTo(5);
        assertThat(response.status()).isEqualTo(TruckStatus.AVAILABLE);
    }

    @Test
    void publishesTruckRegisteredEvent() {
        registerTruck.execute(new CreateTruckRequest("Truck 01", 0, 0, 10));

        verify(eventPublisher).publish(any(TruckRegisteredEvent.class));
    }

    @Test
    void publishesTruckStatusChangedEventWithTruckRegisteredReason() {
        registerTruck.execute(new CreateTruckRequest("Truck 01", 0, 0, 10));

        ArgumentCaptor<TruckStatusChangedEvent> captor = ArgumentCaptor.forClass(TruckStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        TruckStatusChangedEvent event = captor.getValue();
        assertThat(event.getOldStatus()).isNull();
        assertThat(event.getNewStatus()).isEqualTo(TruckStatus.AVAILABLE);
        assertThat(event.getReason()).isEqualTo("TRUCK_REGISTERED");
    }

    @Test
    void publishesBothEventsOnRegistration() {
        registerTruck.execute(new CreateTruckRequest("Truck 01", 0, 0, 10));

        verify(eventPublisher, times(1)).publish(any(TruckRegisteredEvent.class));
        verify(eventPublisher, times(1)).publish(any(TruckStatusChangedEvent.class));
    }
}
