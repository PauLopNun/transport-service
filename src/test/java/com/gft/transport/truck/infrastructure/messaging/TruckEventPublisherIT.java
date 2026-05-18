package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.truck.domain.Location;
import com.gft.transport.truck.domain.TruckId;
import com.gft.transport.truck.domain.TruckStatus;
import com.gft.transport.truck.domain.event.TruckDeletedEvent;
import com.gft.transport.truck.domain.event.TruckRegisteredEvent;
import com.gft.transport.truck.domain.event.TruckStatusChangedEvent;
import com.gft.transport.truck.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("docker")
class TruckEventPublisherIT {

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
    private RabbitMQTruckEventPublisher publisher;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        Queue statusChangedQueue = new Queue("test.truck.status.changed", false, false, true);
        rabbitAdmin.declareQueue(statusChangedQueue);
        rabbitAdmin.declareBinding(new Binding(
                "test.truck.status.changed",
                Binding.DestinationType.QUEUE,
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.status.changed.v1",
                null
        ));

        Queue registeredQueue = new Queue("test.truck.registered", false, false, true);
        rabbitAdmin.declareQueue(registeredQueue);
        rabbitAdmin.declareBinding(new Binding(
                "test.truck.registered",
                Binding.DestinationType.QUEUE,
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.registered.v1",
                null
        ));

        Queue deletedQueue = new Queue("test.truck.deleted", false, false, true);
        rabbitAdmin.declareQueue(deletedQueue);
        rabbitAdmin.declareBinding(new Binding(
                "test.truck.deleted",
                Binding.DestinationType.QUEUE,
                RabbitMQConfig.TRUCKS_EXCHANGE,
                "truck.deleted.v1",
                null
        ));
    }

    @Test
    void publishesStatusChangedEventAsJsonToRabbitMQ() {
        TruckId truckId = TruckId.generate();
        TruckStatusChangedEvent event = new TruckStatusChangedEvent(
                truckId,
                TruckStatus.AVAILABLE,
                TruckStatus.IN_TRANSIT,
                new Location(3, 7),
                6, 10, 3, "DISPATCHED"
        );

        publisher.publish(event);

        Message message = rabbitTemplate.receive("test.truck.status.changed", 3000);
        assertThat(message).isNotNull();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        assertThat(body)
                .contains(truckId.value().toString())
                .contains("DISPATCHED")
                .contains("IN_TRANSIT")
                .contains("AVAILABLE");
    }

    @Test
    void publishesDeletedEventAsJsonToRabbitMQ() {
        TruckId truckId = TruckId.generate();
        TruckDeletedEvent event = new TruckDeletedEvent(truckId);

        publisher.publish(event);

        Message message = rabbitTemplate.receive("test.truck.deleted", 3000);
        assertThat(message).isNotNull();
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).contains(truckId.value().toString());
    }

    @Test
    void publishesRegisteredEventAsJsonToRabbitMQ() {
        TruckId truckId = TruckId.generate();
        TruckRegisteredEvent event = new TruckRegisteredEvent(
                truckId, "Truck-1", new Location(0, 0), 10, 0
        );

        publisher.publish(event);

        Message message = rabbitTemplate.receive("test.truck.registered", 3000);
        assertThat(message).isNotNull();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        assertThat(body)
                .contains(truckId.value().toString())
                .contains("Truck-1");
    }
}
