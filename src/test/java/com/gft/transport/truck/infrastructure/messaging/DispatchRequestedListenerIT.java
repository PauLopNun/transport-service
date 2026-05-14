package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.delivery.infrastructure.persistence.DeliveryJpaRepository;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckPositionUpdatedEvent;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.domain.repository.TruckRepository;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.persistence.TruckJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("docker")
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DispatchRequestedListenerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("transport_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        registry.add("spring.rabbitmq.ssl.enabled", () -> "false");
    }

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private TruckRepository truckRepository;
    @Autowired private TruckJpaRepository truckJpaRepository;
    @Autowired private DeliveryJpaRepository deliveryJpaRepository;
    @Autowired private LocationResolver locationResolver;
    @Autowired private DispatchRequestedListener dispatchRequestedListener;
    @Autowired private WarehouseRegisteredListener warehouseRegisteredListener;
    @Autowired private RabbitMQTruckEventPublisher truckEventPublisher;

    @BeforeEach
    void setUp() {
        deliveryJpaRepository.deleteAll();
        truckJpaRepository.deleteAll();
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare(RabbitMQConfig.SHIPMENTS_EXCHANGE, "topic", true);
            channel.queueBind(RabbitMQConfig.SHIPMENT_REQUESTED_QUEUE, RabbitMQConfig.SHIPMENTS_EXCHANGE, RabbitMQConfig.SHIPMENT_REQUESTED_ROUTING_KEY);
            channel.exchangeDeclare(RabbitMQConfig.WAREHOUSES_EXCHANGE, "topic", true);
            channel.queueBind(RabbitMQConfig.WAREHOUSE_REGISTERED_QUEUE, RabbitMQConfig.WAREHOUSES_EXCHANGE, RabbitMQConfig.WAREHOUSE_REGISTERED_ROUTING_KEY);
            channel.queuePurge(RabbitMQConfig.SHIPMENT_REQUESTED_QUEUE);
            channel.queuePurge(RabbitMQConfig.WAREHOUSE_REGISTERED_QUEUE);
            return null;
        });
    }

    @Nested
    class DispatchRequestedListenerTests {

        @Test
        void assignsTruckWhenShipmentRequestedMessageArrives() {
            TruckId truckId = TruckId.generate();
            UUID shipmentId = UUID.randomUUID();

            truckRepository.save(availableTruck(truckId, "Truck-1"));
            locationResolver.register("warehouse-north-01", new Location(0, 5));
            locationResolver.register("warehouse-south-03", new Location(10, 10));

            publishShipmentRequested(shipmentId, "warehouse-north-01", "warehouse-south-03", UUID.randomUUID().toString(), 3, 2);

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                var truck = truckJpaRepository.findById(truckId.value()).orElseThrow();
                assertThat(truck.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
                assertThat(truck.getCurrentLoad()).isEqualTo(3);

                var deliveries = deliveryJpaRepository.findAll();
                assertThat(deliveries).hasSize(1);
                assertThat(deliveries.get(0).getShipmentId()).isEqualTo(shipmentId);
            });
        }

        @Test
        void assignsTruckWhenShipmentMessageIsDoubleEncodedJson() {
            TruckId truckId = TruckId.generate();
            UUID shipmentId = UUID.randomUUID();

            truckRepository.save(availableTruck(truckId, "Truck-2"));
            locationResolver.register("warehouse-north-01", new Location(0, 5));
            locationResolver.register("warehouse-south-03", new Location(10, 10));

            String innerJson = String.format(
                    "{\"shipmentId\":\"%s\",\"originId\":\"warehouse-north-01\",\"destinationId\":\"warehouse-south-03\"," +
                    "\"items\":[{\"productId\":\"wood\",\"quantity\":4}],\"requestedAt\":1}",
                    shipmentId
            );
            dispatchRequestedListener.onMessage(doubleEncoded(innerJson));

            var truck = truckJpaRepository.findById(truckId.value()).orElseThrow();
            assertThat(truck.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
            assertThat(truck.getCurrentLoad()).isEqualTo(4);
            assertThat(deliveryJpaRepository.findAll()).hasSize(1)
                    .first().extracting(d -> d.getShipmentId()).isEqualTo(shipmentId);
        }

        @Test
        void rejectsInvalidShipmentRequestedMessage() {
            assertThatThrownBy(() -> dispatchRequestedListener.onMessage(invalidMessage()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid shipment.requested.v1 message");
        }
    }

    @Nested
    class WarehouseRegisteredListenerTests {

        @Test
        void registersWarehouseLocationWhenWarehouseRegisteredMessageArrives() {
            String json = "{\"warehouseId\":\"warehouse-X\",\"location\":{\"x\":3,\"y\":7}}";
            rabbitTemplate.send(
                    RabbitMQConfig.WAREHOUSES_EXCHANGE,
                    RabbitMQConfig.WAREHOUSE_REGISTERED_ROUTING_KEY,
                    jsonMessage(json)
            );

            await().atMost(Duration.ofSeconds(5)).ignoreExceptions().untilAsserted(() ->
                    assertThat(locationResolver.resolve("warehouse-X")).isEqualTo(new Location(3, 7))
            );
        }

        @Test
        void registersWarehouseLocationWhenWarehouseMessageIsDoubleEncodedJson() {
            String innerJson = "{\"warehouseId\":\"warehouse-Y\",\"location\":{\"x\":5,\"y\":2}}";
            warehouseRegisteredListener.onMessage(doubleEncoded(innerJson));

            assertThat(locationResolver.resolve("warehouse-Y")).isEqualTo(new Location(5, 2));
        }

        @Test
        void rejectsInvalidWarehouseRegisteredMessage() {
            assertThatThrownBy(() -> warehouseRegisteredListener.onMessage(invalidMessage()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid warehouse.registered.v1 message");
        }
    }

    @Nested
    class LocationResolverTests {

        @Test
        void throwsExceptionWhenResolvingUnknownWarehouseId() {
            assertThatThrownBy(() -> locationResolver.resolve("unknown-warehouse"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown warehouse ID: unknown-warehouse");
        }
    }

    @Nested
    class RabbitMQTruckEventPublisherTests {

        @Test
        void publishesTruckRegisteredEvent() {
            TruckRegisteredEvent event = new TruckRegisteredEvent(
                    TruckId.generate(), "Truck-X", new Location(0, 0), 10, 0
            );
            assertThatCode(() -> truckEventPublisher.publish(event)).doesNotThrowAnyException();
        }

        @Test
        void publishesTruckStatusChangedEventWithNullOldStatus() {
            TruckStatusChangedEvent event = new TruckStatusChangedEvent(
                    TruckId.generate(), null, TruckStatus.IN_TRANSIT,
                    new Location(0, 0), 0, 10, 0, "TRUCK_REGISTERED"
            );
            assertThatCode(() -> truckEventPublisher.publish(event)).doesNotThrowAnyException();
        }

        @Test
        void publishesTruckPositionUpdatedEvent() {
            TruckPositionUpdatedEvent event = new TruckPositionUpdatedEvent(
                    TruckId.generate(), new Location(5, 3)
            );
            assertThatCode(() -> truckEventPublisher.publish(event)).doesNotThrowAnyException();
        }
    }

    private Truck availableTruck(TruckId truckId, String name) {
        return Truck.builder()
                .truckId(truckId)
                .name(name)
                .location(new Location(0, 0))
                .status(TruckStatus.AVAILABLE)
                .capacity(10)
                .currentLoad(0)
                .deliveryIds(List.of())
                .build();
    }

    private void publishShipmentRequested(UUID shipmentId, String originId, String destinationId,
                                          String productId, int quantity, int requestedAt) {
        String json = String.format(
                "{\"shipmentId\":\"%s\",\"originId\":\"%s\",\"destinationId\":\"%s\"," +
                "\"items\":[{\"productId\":\"%s\",\"quantity\":%d}],\"requestedAt\":%d}",
                shipmentId, originId, destinationId, productId, quantity, requestedAt
        );
        rabbitTemplate.send(
                RabbitMQConfig.SHIPMENTS_EXCHANGE,
                RabbitMQConfig.SHIPMENT_REQUESTED_ROUTING_KEY,
                jsonMessage(json)
        );
    }

    private Message jsonMessage(String json) {
        return MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
    }

    private Message doubleEncoded(String innerJson) {
        String wrapped = "\"" + innerJson.replace("\"", "\\\"") + "\"";
        return jsonMessage(wrapped);
    }

    private Message invalidMessage() {
        return jsonMessage("not-json");
    }
}
