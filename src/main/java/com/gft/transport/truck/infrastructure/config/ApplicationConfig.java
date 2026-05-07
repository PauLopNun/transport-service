package com.gft.transport.truck.infrastructure.config;

import com.gft.transport.truck.domain.service.DistanceCalculator;
import com.gft.transport.truck.domain.service.OptimalTruckSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public DistanceCalculator distanceCalculator() {
        return new DistanceCalculator();
    }

    @Bean
    public OptimalTruckSelector optimalTruckSelector(DistanceCalculator distanceCalculator) {
        return new OptimalTruckSelector(distanceCalculator);
    }
}
