package com.gft.transport.truck.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    // --- Exchanges ---

    @Test
    void trucksExchangeHasCorrectName() {
        TopicExchange exchange = config.trucksExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.TRUCKS_EXCHANGE);
    }

    @Test
    void shipmentsExchangeHasCorrectName() {
        TopicExchange exchange = config.shipmentsExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.SHIPMENTS_EXCHANGE);
    }

    @Test
    void simulationExchangeHasCorrectName() {
        TopicExchange exchange = config.simulationExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.SIMULATION_EXCHANGE);
    }

    // --- Queues ---

    @Test
    void shipmentRequestedQueueHasCorrectName() {
        Queue queue = config.shipmentRequestedQueue();
        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.SHIPMENT_REQUESTED_QUEUE);
    }

    @Test
    void timeAdvancedQueueHasCorrectName() {
        Queue queue = config.timeAdvancedQueue();
        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.TIME_ADVANCED_QUEUE);
    }

    // --- Bindings ---

    @Test
    void shipmentRequestedBindingConnectsQueueToShipmentsExchange() {
        Queue queue = config.shipmentRequestedQueue();
        TopicExchange exchange = config.shipmentsExchange();

        Binding binding = config.shipmentRequestedBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.SHIPMENT_REQUESTED_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.SHIPMENTS_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.SHIPMENT_REQUESTED_ROUTING_KEY);
    }

    @Test
    void timeAdvancedBindingConnectsQueueToSimulationExchange() {
        Queue queue = config.timeAdvancedQueue();
        TopicExchange exchange = config.simulationExchange();

        Binding binding = config.timeAdvancedBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.TIME_ADVANCED_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.SIMULATION_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.TIME_ADVANCED_ROUTING_KEY);
    }
}
