package com.gft.transport.truck.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRUCKS_EXCHANGE                = "trucks.exchange";
    public static final String SHIPMENTS_EXCHANGE             = "shipments.exchange";
    public static final String SIMULATION_EXCHANGE            = "ms-time.exchange";
    public static final String WAREHOUSES_EXCHANGE            = "warehouses.exchange";

    public static final String SHIPMENT_REQUESTED_QUEUE       = "trucks.shipment.requested";
    public static final String TIME_ADVANCED_QUEUE            = "trucks.time.advanced";
    public static final String WAREHOUSE_REGISTERED_QUEUE     = "trucks.warehouse.registered";

    public static final String SHIPMENT_REQUESTED_ROUTING_KEY = "shipment.requested.v1";
    public static final String TIME_ADVANCED_ROUTING_KEY      = "time.advanced.v1";
    public static final String WAREHOUSE_REGISTERED_ROUTING_KEY = "warehouse.registered.v1";

    @Value("${transport.rabbitmq.declare-external-exchanges:false}")
    private boolean declareExternalExchanges;

    @Bean
    public TopicExchange trucksExchange() {
        return new TopicExchange(TRUCKS_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentsExchange() {
        TopicExchange exchange = new TopicExchange(SHIPMENTS_EXCHANGE);
        exchange.setShouldDeclare(declareExternalExchanges);
        return exchange;
    }

    @Bean
    public TopicExchange simulationExchange() {
        TopicExchange exchange = new TopicExchange(SIMULATION_EXCHANGE);
        exchange.setShouldDeclare(declareExternalExchanges);
        return exchange;
    }

    @Bean
    public TopicExchange warehousesExchange() {
        TopicExchange exchange = new TopicExchange(WAREHOUSES_EXCHANGE);
        exchange.setShouldDeclare(declareExternalExchanges);
        return exchange;
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
        // Used by listener containers. Our listeners handle JSON deserialization
        // manually via ObjectMapper, so no type-header resolution needed here.
        return new SimpleMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // Publishing converter: serialize to JSON without __TypeId__ header
        // so cross-service consumers (map, reporting) can deserialize with their own classes.
        Jackson2JsonMessageConverter publishConverter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        publishConverter.setJavaTypeMapper(typeMapper);
        template.setMessageConverter(publishConverter);
        return template;
    }
}
