package com.gft.transport.truck.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRUCKS_EXCHANGE                = "trucks.exchange";
    public static final String SHIPMENTS_EXCHANGE             = "shipments.exchange";
    public static final String SIMULATION_EXCHANGE            = "simulation.exchange";
    public static final String WAREHOUSES_EXCHANGE            = "warehouses.exchange";

    public static final String SHIPMENT_REQUESTED_QUEUE       = "trucks.shipment.requested";
    public static final String TIME_ADVANCED_QUEUE            = "trucks.time.advanced";
    public static final String WAREHOUSE_REGISTERED_QUEUE     = "trucks.warehouse.registered";

    public static final String SHIPMENT_REQUESTED_ROUTING_KEY = "shipment.requested.v1";
    public static final String TIME_ADVANCED_ROUTING_KEY      = "time.advanced.v1";
    public static final String WAREHOUSE_REGISTERED_ROUTING_KEY = "warehouse.registered.v1";

    @Bean
    public TopicExchange trucksExchange() {
        return new TopicExchange(TRUCKS_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentsExchange() {
        return new TopicExchange(SHIPMENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange simulationExchange() {
        return new TopicExchange(SIMULATION_EXCHANGE);
    }

    @Bean
    public TopicExchange warehousesExchange() {
        return new TopicExchange(WAREHOUSES_EXCHANGE);
    }

    @Bean
    public Queue shipmentRequestedQueue() {
        return new Queue(SHIPMENT_REQUESTED_QUEUE);
    }

    @Bean
    public Queue timeAdvancedQueue() {
        return new Queue(TIME_ADVANCED_QUEUE);
    }

    @Bean
    public Queue warehouseRegisteredQueue() {
        return new Queue(WAREHOUSE_REGISTERED_QUEUE);
    }

    @Bean
    public Binding shipmentRequestedBinding(Queue shipmentRequestedQueue, TopicExchange shipmentsExchange) {
        return BindingBuilder.bind(shipmentRequestedQueue)
                .to(shipmentsExchange)
                .with(SHIPMENT_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public Binding timeAdvancedBinding(Queue timeAdvancedQueue, TopicExchange simulationExchange) {
        return BindingBuilder.bind(timeAdvancedQueue)
                .to(simulationExchange)
                .with(TIME_ADVANCED_ROUTING_KEY);
    }

    @Bean
    public Binding warehouseRegisteredBinding(Queue warehouseRegisteredQueue, TopicExchange warehousesExchange) {
        return BindingBuilder.bind(warehouseRegisteredQueue)
                .to(warehousesExchange)
                .with(WAREHOUSE_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
