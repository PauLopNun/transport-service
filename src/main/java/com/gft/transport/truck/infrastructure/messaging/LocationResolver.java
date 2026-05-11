package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.truck.domain.Location;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocationResolver {

    private final Map<String, Location> locations = new ConcurrentHashMap<>();

    public void register(String warehouseId, Location location) {
        locations.put(warehouseId, location);
    }

    public Location resolve(String warehouseId) {
        Location location = locations.get(warehouseId);
        if (location == null) {
            throw new IllegalArgumentException("Unknown warehouse ID: " + warehouseId);
        }
        return location;
    }
}
