package com.gft.transport.truck.infrastructure.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Transport Service API",
                version = "1.0.0-MVP",
                description = """
                        Manages truck fleet, shipment assignment and delivery tracking. Supply Chain Workshop - GFT 2026.

                        Messaging contracts:

                        Transport publishes domain events to RabbitMQ exchanges. The publisher only sends a message to an exchange with a routing key; consumer services own their queues and bindings.

                        Published by Transport:
                        - trucks.exchange + truck.registered.v1
                        - trucks.exchange + truck.status.changed.v1
                        - trucks.exchange + truck.position.updated.v1
                        - shipments.exchange + delivery.completed.v1

                        Consumed by Transport:
                        - shipments.exchange + shipment.requested.v1 -> trucks.shipment.requested
                        - simulation.exchange + time.advanced.v1 -> trucks.time.advanced

                        Important: Reporting, Map UI and other consumers must declare their own queues and bind them to the exchange/routing keys they need. Transport does not create bindings for other services.
                        """,
                contact = @Contact(name = "team-trucks", email = "paulopeznunez@gmail.com")
        ),
        servers = @Server(url = "http://localhost:8080")
)
public class OpenApiConfig {}
