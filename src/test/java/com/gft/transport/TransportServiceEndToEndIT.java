package com.gft.transport;

import com.gft.transport.delivery.infrastructure.persistence.DeliveryJpaRepository;
import com.gft.transport.truck.application.dto.CreateTruckRequest;
import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.messaging.LocationResolver;
import com.gft.transport.truck.infrastructure.persistence.TruckJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("local")
class TransportServiceEndToEndIT {

    private static final String TRUCK_REGISTERED_CAPTURE   = "e2e.capture.truck.registered";
    private static final String TRUCK_STATUS_CAPTURE       = "e2e.capture.truck.status.changed";
    private static final String DELIVERY_COMPLETED_CAPTURE = "e2e.capture.delivery.completed";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
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

    @Autowired TestRestTemplate restTemplate;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RabbitAdmin rabbitAdmin;
    @Autowired TruckJpaRepository truckJpaRepository;
    @Autowired DeliveryJpaRepository deliveryJpaRepository;
    @Autowired LocationResolver locationResolver;

    @BeforeEach
    void setUp() {
        deliveryJpaRepository.deleteAll();
        truckJpaRepository.deleteAll();

        declareCaptureQueue(TRUCK_REGISTERED_CAPTURE, RabbitMQConfig.TRUCKS_EXCHANGE, "truck.registered.v1");
        declareCaptureQueue(TRUCK_STATUS_CAPTURE, RabbitMQConfig.TRUCKS_EXCHANGE, "truck.status.changed.v1");
        declareCaptureQueue(DELIVERY_COMPLETED_CAPTURE, RabbitMQConfig.SHIPMENTS_EXCHANGE, "delivery.completed.v1");

        purgeQueues(
                RabbitMQConfig.SHIPMENT_REQUESTED_QUEUE,
                RabbitMQConfig.WAREHOUSE_REGISTERED_QUEUE,
                RabbitMQConfig.TIME_ADVANCED_QUEUE,
                TRUCK_REGISTERED_CAPTURE,
                TRUCK_STATUS_CAPTURE,
                DELIVERY_COMPLETED_CAPTURE
        );
    }

    @Test
    void completesFullDeliveryFlowFromTruckRegistrationToDeliveryCompletion() {
        CreateTruckRequest createRequest = new CreateTruckRequest("Truck-E2E", 0, 0, 10);
        ResponseEntity<TruckResponse> registerResponse = restTemplate.postForEntity(
                "/trucks", createRequest, TruckResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID truckId = UUID.fromString(registerResponse.getBody().truckId());

        assertTruckRegisteredEventPublished(truckId);
        assertTruckStatusEventPublished("TRUCK_REGISTERED");

        publishWarehouseRegistered("warehouse-origin", 0, 0);
        publishWarehouseRegistered("warehouse-dest", 2, 0);
        awaitWarehousesRegistered("warehouse-origin", "warehouse-dest");

        UUID shipmentId = UUID.randomUUID();
        String productId = UUID.randomUUID().toString();
        publishShipmentRequested(shipmentId, "warehouse-origin", "warehouse-dest", productId, 4, 1);
        awaitTruckDispatched(truckId, shipmentId);
        assertTruckStatusEventPublished("DISPATCHED");

        publishTimeAdvanced(0, 1, 1);
        awaitTruckAtPosition(truckId, 1, 0, TruckStatus.IN_TRANSIT);

        publishTimeAdvanced(1, 2, 1);
        awaitTruckAtPosition(truckId, 2, 0, TruckStatus.AVAILABLE);

        assertDeliveryCompletedEventPublished(shipmentId, truckId, productId);
        assertTruckStatusEventPublished("RETURNED_TO_BASE");
    }

    private void assertTruckRegisteredEventPublished(UUID truckId) {
        Message event = rabbitTemplate.receive(TRUCK_REGISTERED_CAPTURE, 5000);
        assertThat(event).isNotNull();
        assertThat(body(event)).contains(truckId.toString()).contains("Truck-E2E");
    }

    private void assertTruckStatusEventPublished(String expectedReason) {
        Message event = rabbitTemplate.receive(TRUCK_STATUS_CAPTURE, 5000);
        assertThat(event).isNotNull();
        assertThat(body(event)).contains(expectedReason);
    }

    private void assertDeliveryCompletedEventPublished(UUID shipmentId, UUID truckId, String productId) {
        Message event = rabbitTemplate.receive(DELIVERY_COMPLETED_CAPTURE, 5000);
        assertThat(event).isNotNull();
        assertThat(body(event))
                .contains(shipmentId.toString())
                .contains(truckId.toString())
                .contains(productId);
    }

    private void awaitWarehousesRegistered(String... warehouseIds) {
        await().atMost(Duration.ofSeconds(5))
               .ignoreException(IllegalArgumentException.class)
               .untilAsserted(() -> {
                   for (String id : warehouseIds) {
                       locationResolver.resolve(id);
                   }
               });
    }

    private void awaitTruckDispatched(UUID truckId, UUID shipmentId) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var truck = truckJpaRepository.findById(truckId).orElseThrow();
            assertThat(truck.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
            assertThat(truck.getCurrentLoad()).isEqualTo(4);
            assertThat(deliveryJpaRepository.findAll()).hasSize(1)
                    .first().extracting(d -> d.getShipmentId()).isEqualTo(shipmentId);
        });
    }

    private void awaitTruckAtPosition(UUID truckId, int expectedX, int expectedY, TruckStatus expectedStatus) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var truck = truckJpaRepository.findById(truckId).orElseThrow();
            assertThat(truck.getX()).isEqualTo(expectedX);
            assertThat(truck.getY()).isEqualTo(expectedY);
            assertThat(truck.getStatus()).isEqualTo(expectedStatus);
        });
    }

    private void declareCaptureQueue(String queueName, String exchange, String routingKey) {
        rabbitAdmin.declareQueue(new Queue(queueName, false, false, false));
        rabbitAdmin.declareBinding(new Binding(queueName, Binding.DestinationType.QUEUE, exchange, routingKey, null));
    }

    private void purgeQueues(String... queues) {
        rabbitTemplate.execute(channel -> {
            for (String queue : queues) {
                channel.queuePurge(queue);
            }
            return null;
        });
    }

    private void publishWarehouseRegistered(String warehouseId, int x, int y) {
        String json = String.format(
                "{\"warehouseId\":\"%s\",\"location\":{\"x\":%d,\"y\":%d}}",
                warehouseId, x, y
        );
        rabbitTemplate.send(
                RabbitMQConfig.WAREHOUSES_EXCHANGE,
                RabbitMQConfig.WAREHOUSE_REGISTERED_ROUTING_KEY,
                jsonMessage(json)
        );
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

    private void publishTimeAdvanced(int previousDay, int currentDay, int daysAdvanced) {
        String json = String.format(
                "{\"eventId\":\"%s\",\"previousDay\":%d,\"currentDay\":%d,\"daysAdvanced\":%d,\"occurredAt\":\"%s\"}",
                UUID.randomUUID(), previousDay, currentDay, daysAdvanced, Instant.now()
        );
        rabbitTemplate.send(
                RabbitMQConfig.SIMULATION_EXCHANGE,
                RabbitMQConfig.TIME_ADVANCED_ROUTING_KEY,
                jsonMessage(json)
        );
    }

    private Message jsonMessage(String json) {
        return MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
    }

    private String body(Message message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }
}
