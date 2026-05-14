package com.gft.transport.truck.infrastructure.persistence;

import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("docker")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TruckRepositoryAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("transport_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TruckJpaRepository jpaRepository;

    private TruckRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TruckRepositoryAdapter(jpaRepository);
    }

    @Test
    void savesAndFindsTruckById() {
        Truck truck = buildTruck(TruckStatus.AVAILABLE);

        adapter.save(truck);

        Optional<Truck> found = adapter.findById(truck.getTruckId());
        assertThat(found).isPresent();
        assertThat(found.get().getTruckId()).isEqualTo(truck.getTruckId());
        assertThat(found.get().getName()).isEqualTo("Truck-1");
        assertThat(found.get().getLocation()).isEqualTo(new Location(3, 7));
        assertThat(found.get().getStatus()).isEqualTo(TruckStatus.AVAILABLE);
        assertThat(found.get().getCapacity()).isEqualTo(100);
        assertThat(found.get().getCurrentLoad()).isEqualTo(0);
    }

    @Test
    void returnsEmptyWhenTruckNotFound() {
        assertThat(adapter.findById(TruckId.generate())).isEmpty();
    }

    @Test
    void findAllReturnsAllSavedTrucks() {
        adapter.save(buildTruck(TruckStatus.AVAILABLE));
        adapter.save(buildTruck(TruckStatus.IN_TRANSIT));

        assertThat(adapter.findAll()).hasSize(2);
    }

    @Test
    void findAvailableReturnsOnlyAvailableTrucks() {
        adapter.save(buildTruck(TruckStatus.AVAILABLE));
        adapter.save(buildTruck(TruckStatus.IN_TRANSIT));
        adapter.save(buildTruck(TruckStatus.DELIVERED));

        List<Truck> available = adapter.findAvailable();

        assertThat(available).hasSize(1);
        assertThat(available.get(0).getStatus()).isEqualTo(TruckStatus.AVAILABLE);
    }

    @Test
    void saveUpdatesExistingTruck() {
        Truck truck = buildTruck(TruckStatus.AVAILABLE);
        adapter.save(truck);

        Truck updated = Truck.builder()
                .truckId(truck.getTruckId())
                .name(truck.getName())
                .location(new Location(9, 9))
                .status(TruckStatus.IN_TRANSIT)
                .capacity(100)
                .currentLoad(40)
                .deliveryIds(List.of())
                .build();
        adapter.save(updated);

        Truck found = adapter.findById(truck.getTruckId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
        assertThat(found.getLocation()).isEqualTo(new Location(9, 9));
        assertThat(found.getCurrentLoad()).isEqualTo(40);
    }

    @Test
    void savesAndRestoresDeliveryIds() {
        DeliveryId d1 = DeliveryId.generate();
        DeliveryId d2 = DeliveryId.generate();

        Truck truck = Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck-1")
                .location(new Location(0, 0))
                .status(TruckStatus.IN_TRANSIT)
                .capacity(100)
                .currentLoad(50)
                .deliveryIds(List.of(d1, d2))
                .build();
        adapter.save(truck);

        Truck found = adapter.findById(truck.getTruckId()).orElseThrow();
        assertThat(found.getDeliveryIds()).containsExactlyInAnyOrder(d1, d2);
    }

    private Truck buildTruck(TruckStatus status) {
        return Truck.builder()
                .truckId(TruckId.generate())
                .name("Truck-1")
                .location(new Location(3, 7))
                .status(status)
                .capacity(100)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();
    }
}
