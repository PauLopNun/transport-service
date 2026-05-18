package com.gft.transport.truck.infrastructure.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of(
                "service", "transport-service",
                "status", "UP",
                "trucks", "/trucks",
                "swagger", "/swagger-ui/index.html"
        );
    }
}
