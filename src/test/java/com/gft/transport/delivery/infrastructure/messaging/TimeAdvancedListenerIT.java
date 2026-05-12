package com.gft.transport.delivery.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.delivery.application.port.out.DeliveryEventPublisher;
import com.gft.transport.delivery.domain.Delivery;
import com.gft.transport.delivery.domain.DeliveryId;
import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.delivery.domain.repository.DeliveryRepository;
import com.gft.transport.delivery.infrastructure.persistence.DeliveryJpaRepository;
import com.gft.transport.truck.application.port.out.TruckEventPublisher;
import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.Truck;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.repository.TruckRepository;
import com.gft.transport.truck.domain.service.OptimalTruckSelector;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import com.gft.transport.truck.infrastructure.persistence.TruckJpaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TimeAdvancedListenerIT {

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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private TruckJpaRepository truckJpaRepository;

    @Autowired
    private DeliveryJpaRepository deliveryJpaRepository;

    @Autowired
    private TimeAdvancedListener timeAdvancedListener;

    @MockitoBean
    private TruckEventPublisher truckEventPublisher;

    @MockitoBean
    private DeliveryEventPublisher deliveryEventPublisher;

    @MockitoBean
    private OptimalTruckSelector optimalTruckSelector;

    @BeforeEach
    void setUp() {
        deliveryJpaRepository.deleteAll();
        truckJpaRepository.deleteAll();
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare(RabbitMQConfig.SIMULATION_EXCHANGE, "topic", true);
            channel.queueBind(RabbitMQConfig.TIME_ADVANCED_QUEUE, RabbitMQConfig.SIMULATION_EXCHANGE, RabbitMQConfig.TIME_ADVANCED_ROUTING_KEY);
            channel.queuePurge(RabbitMQConfig.TIME_ADVANCED_QUEUE);
            return null;
        });
    }

    @Test
    void deserializesAllFieldsFromMsSimulationFormat() throws Exception {
        String json = """
                {"eventId":"abc","previousDay":1,"currentDay":3,"daysAdvanced":2,"occurredAt":"2026-05-11T10:00:00Z"}
                """;

        TimeAdvancedMessage msg = objectMapper.readValue(json, TimeAdvancedMessage.class);

        assertThat(msg.previousDayNumber()).as("previousDay").isEqualTo(1);
        assertThat(msg.currentDayNumber()).as("currentDay").isEqualTo(3);
        assertThat(msg.daysAdvanced()).as("daysAdvanced").isEqualTo(2);
    }

    @Test
    void advancesTruckWhenDaysAdvancedIsOne() {
        TruckId truckId = TruckId.generate();
        DeliveryId deliveryId = DeliveryId.generate();

        seedInTransitTruck(truckId, deliveryId);
        seedPendingDelivery(truckId, deliveryId);

        publishTimeAdvanced(1, 2, 1);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var truck = truckJpaRepository.findById(truckId.value()).orElseThrow();
            assertThat(truck.getX()).isEqualTo(1);
            assertThat(truck.getY()).isEqualTo(0);
            assertThat(truck.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
        });
    }

    @Test
    void advancesMultipleStepsWhenDaysAdvancedIsGreaterThanOne() {
        TruckId truckId = TruckId.generate();
        DeliveryId deliveryId = DeliveryId.generate();

        seedInTransitTruck(truckId, deliveryId);
        seedPendingDelivery(truckId, deliveryId);

        publishTimeAdvanced(1, 3, 2);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var truck = truckJpaRepository.findById(truckId.value()).orElseThrow();
            assertThat(truck.getX()).isEqualTo(2);
            assertThat(truck.getY()).isEqualTo(0);
            assertThat(truck.getStatus()).isEqualTo(TruckStatus.AVAILABLE);
            var delivery = deliveryJpaRepository.findById(deliveryId.value()).orElseThrow();
            assertThat(delivery.getCompletedAt()).isEqualTo(3);
        });
    }

    @Test
    void doesNotAdvanceTruckWhenDaysAdvancedIsZero() {
        TruckId truckId = TruckId.generate();
        DeliveryId deliveryId = DeliveryId.generate();

        seedInTransitTruck(truckId, deliveryId);
        seedPendingDelivery(truckId, deliveryId);

        publishTimeAdvanced(1, 1, 0);

        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            var truck = truckJpaRepository.findById(truckId.value()).orElseThrow();
            assertThat(truck.getX()).isEqualTo(0);
            assertThat(truck.getY()).isEqualTo(0);
            assertThat(truck.getStatus()).isEqualTo(TruckStatus.IN_TRANSIT);
        });
    }

    @Test
    void rejectsInvalidTimeAdvancedMessage() {
        Message message = MessageBuilder
                .withBody("not-json".getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();

        assertThatThrownBy(() -> timeAdvancedListener.onMessage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid time.advanced.v1 message");
    }

    private void seedInTransitTruck(TruckId truckId, DeliveryId deliveryId) {
        truckRepository.save(Truck.builder()
                .truckId(truckId)
                .name("Truck-1")
                .location(new Location(0, 0))
                .status(TruckStatus.IN_TRANSIT)
                .capacity(10)
                .currentLoad(2)
                .deliveryIds(List.of(deliveryId))
                .build());
    }

    private void seedPendingDelivery(TruckId truckId, DeliveryId deliveryId) {
        deliveryRepository.save(Delivery.builder()
                .deliveryId(deliveryId)
                .shipmentId(UUID.randomUUID())
                .truckId(truckId)
                .origin(new Location(0, 0))
                .destination(new Location(2, 0))
                .items(List.of(new DeliveryItem("wood", 2)))
                .assignedAt(1)
                .completedAt(null)
                .build());
    }

    private void publishTimeAdvanced(int previousDayNumber, int currentDayNumber, int daysAdvanced) {
        String json = String.format(
                "{\"eventId\":\"%s\",\"previousDay\":%d,\"currentDay\":%d,\"daysAdvanced\":%d,\"occurredAt\":\"%s\"}",
                UUID.randomUUID(), previousDayNumber, currentDayNumber, daysAdvanced, Instant.now()
        );
        Message message = MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
        rabbitTemplate.send(
                RabbitMQConfig.SIMULATION_EXCHANGE,
                RabbitMQConfig.TIME_ADVANCED_ROUTING_KEY,
                message
        );
    }
}
