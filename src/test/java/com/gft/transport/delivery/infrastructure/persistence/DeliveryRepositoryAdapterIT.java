package com.gft.transport.delivery.infrastructure.persistence;

import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeliveryRepositoryAdapterIT {

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
    private DeliveryJpaRepository jpaRepository;

    private DeliveryRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DeliveryRepositoryAdapter(jpaRepository);
    }

    @Test
    void savesAndFindsDeliveryById() {
        Delivery delivery = buildDelivery();

        adapter.save(delivery);

        Optional<Delivery> found = adapter.findById(delivery.getDeliveryId());
        assertThat(found).isPresent();
        assertThat(found.get().getDeliveryId()).isEqualTo(delivery.getDeliveryId());
        assertThat(found.get().getShipmentId()).isEqualTo(delivery.getShipmentId());
    }

    @Test
    void findsDeliveriesByTruckId() {
        TruckId truckId = TruckId.generate();
        Delivery delivery1 = buildDeliveryForTruck(truckId);
        Delivery delivery2 = buildDeliveryForTruck(truckId);
        Delivery other = buildDelivery();

        adapter.save(delivery1);
        adapter.save(delivery2);
        adapter.save(other);

        List<Delivery> found = adapter.findByTruckId(truckId);
        assertThat(found).hasSize(2);
    }

    @Test
    void returnsEmptyWhenDeliveryNotFound() {
        assertThat(adapter.findById(DeliveryId.generate())).isEmpty();
    }

    @Test
    void savesDeliveryWithItems() {
        Delivery delivery = buildDelivery();
        adapter.save(delivery);

        Delivery found = adapter.findById(delivery.getDeliveryId()).orElseThrow();
        assertThat(found.getItems()).hasSize(2);
        assertThat(found.getItems().get(0).productId()).isNotNull();
    }

    private Delivery buildDelivery() {
        return buildDeliveryForTruck(TruckId.generate());
    }

    @Test
    void savesAndRestoresOrigin() {
        Delivery delivery = Delivery.builder()
                .deliveryId(DeliveryId.generate())
                .shipmentId(java.util.UUID.randomUUID())
                .truckId(TruckId.generate())
                .origin(new Location(2, 3))
                .destination(new Location(5, 5))
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 1)))
                .assignedAt(1)
                .completedAt(null)
                .build();

        adapter.save(delivery);

        Delivery found = adapter.findById(delivery.getDeliveryId()).orElseThrow();
        assertThat(found.getOrigin()).isEqualTo(new Location(2, 3));
    }

    @Test
    void savesAndRestoresDeliveryWithNullOrigin() {
        Delivery delivery = Delivery.builder()
                .deliveryId(DeliveryId.generate())
                .shipmentId(java.util.UUID.randomUUID())
                .truckId(TruckId.generate())
                .origin(null)
                .destination(new Location(5, 5))
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 1)))
                .assignedAt(1)
                .completedAt(null)
                .build();

        adapter.save(delivery);

        Delivery found = adapter.findById(delivery.getDeliveryId()).orElseThrow();
        assertThat(found.getOrigin()).isNull();
    }

    private Delivery buildDeliveryForTruck(TruckId truckId) {
        return Delivery.builder()
                .deliveryId(DeliveryId.generate())
                .shipmentId(java.util.UUID.randomUUID())
                .truckId(truckId)
                .origin(new Location(0, 0))
                .destination(new Location(5, 5))
                .items(List.of(new DeliveryItem(UUID.randomUUID(), 6), new DeliveryItem(UUID.randomUUID(), 12)))
                .assignedAt(1)
                .completedAt(null)
                .build();
    }
}
