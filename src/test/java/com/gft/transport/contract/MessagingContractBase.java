package com.gft.transport.contract;

import com.gft.transport.delivery.application.port.out.DeliveryEventPublisher;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckDeletedEvent;
import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("docker")
@AutoConfigureMessageVerifier
public abstract class MessagingContractBase {

    private static final TruckId TRUCK_ID = new TruckId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    private static final UUID SHIPMENT_ID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");

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
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        registry.add("spring.rabbitmq.ssl.enabled", () -> "false");
    }

    @Autowired
    private TruckEventPublisher truckEventPublisher;

    @Autowired
    private DeliveryEventPublisher deliveryEventPublisher;

    public void publishTruckRegistered() {
        truckEventPublisher.publish(new TruckRegisteredEvent(TRUCK_ID, "Truck 01", new Location(0, 0), 10, 0));
    }

    public void publishTruckStatusChanged() {
        truckEventPublisher.publish(new TruckStatusChangedEvent(
                TRUCK_ID, TruckStatus.AVAILABLE, TruckStatus.IN_TRANSIT,
                new Location(0, 0), 6, 10, 3, "DISPATCHED"
        ));
    }

    public void publishTruckPositionUpdated() {
        truckEventPublisher.publish(new TruckPositionUpdatedEvent(TRUCK_ID, new Location(5, 3)));
    }

    public void publishTruckDeleted() {
        truckEventPublisher.publish(new TruckDeletedEvent(TRUCK_ID));
    }

    public void publishDeliveryCompleted() {
        deliveryEventPublisher.publish(new DeliveryCompletedEvent(
                SHIPMENT_ID, TRUCK_ID,
                List.of(new DeliveryItem("wood", 6)),
                new Location(8, 2), 5
        ));
    }
}
