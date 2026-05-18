package com.gft.transport.truck.application.usecase;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.exception.TruckNotFoundException;
import com.gft.transport.truck.domain.repository.TruckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteTruckTest {

    @Mock
    private TruckRepository truckRepository;

    private DeleteTruck deleteTruck;

    @BeforeEach
    void setUp() {
        deleteTruck = new DeleteTruck(truckRepository);
    }

    @Test
    void throwsTruckNotFoundExceptionWhenTruckDoesNotExist() {
        TruckId truckId = TruckId.generate();
        when(truckRepository.findById(truckId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteTruck.execute(truckId))
                .isInstanceOf(TruckNotFoundException.class);

        verify(truckRepository, never()).deleteById(any());
        verify(truckRepository, never()).save(any());
    }

    @Test
    void deletesImmediatelyAndReturnsDeletedWhenTruckIsAvailable() {
        Truck truck = truck(TruckStatus.AVAILABLE, false);
        when(truckRepository.findById(truck.getTruckId())).thenReturn(Optional.of(truck));

        DeleteTruckResult result = deleteTruck.execute(truck.getTruckId());

        assertThat(result).isEqualTo(DeleteTruckResult.DELETED);
        verify(truckRepository).deleteById(truck.getTruckId());
        verify(truckRepository, never()).save(any());
    }

    @Test
    void marksPendingDeletionAndReturnsDeletionScheduledWhenTruckIsInTransit() {
        Truck truck = truck(TruckStatus.IN_TRANSIT, false);
        when(truckRepository.findById(truck.getTruckId())).thenReturn(Optional.of(truck));

        DeleteTruckResult result = deleteTruck.execute(truck.getTruckId());

        assertThat(result).isEqualTo(DeleteTruckResult.DELETION_SCHEDULED);
        ArgumentCaptor<Truck> captor = ArgumentCaptor.forClass(Truck.class);
        verify(truckRepository).save(captor.capture());
        assertThat(captor.getValue().isPendingDeletion()).isTrue();
        verify(truckRepository, never()).deleteById(any());
    }

    @Test
    void returnsDeletionScheduledWhenTruckIsAlreadyPendingDeletion() {
        Truck truck = truck(TruckStatus.IN_TRANSIT, true);
        when(truckRepository.findById(truck.getTruckId())).thenReturn(Optional.of(truck));

        DeleteTruckResult result = deleteTruck.execute(truck.getTruckId());

        assertThat(result).isEqualTo(DeleteTruckResult.DELETION_SCHEDULED);
        verify(truckRepository, never()).deleteById(any());
    }

    private Truck truck(TruckStatus status, boolean pendingDeletion) {
        return Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck-1")
                .location(new Location(0, 0))
                .status(status)
                .capacity(10)
                .currentLoad(0)
                .deliveryIds(List.of())
                .pendingDeletion(pendingDeletion)
                .build();
    }
}
