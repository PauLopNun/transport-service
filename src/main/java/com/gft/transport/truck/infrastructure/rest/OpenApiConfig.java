package com.gft.transport.truck.infrastructure.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Transport Service API",
                version = "1.0.0-MVP",
                description = "Manages truck fleet, shipment assignment and delivery tracking. Supply Chain Workshop — GFT 2026.",
                contact = @Contact(name = "team-trucks", email = "paulopeznunez@gmail.com")
        ),
        servers = @Server(url = "http://localhost:8080")
)
public class OpenApiConfig {}
