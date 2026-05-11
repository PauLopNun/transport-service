package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.truck.domain.Location;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves warehouse/factory string IDs to grid Location coordinates.
 * Coordinates are hardcoded for MVP — confirm values with team-warehouses and team-factories.
 */
@Component
public class LocationResolver {

    private static final Map<String, Location> LOCATIONS = Map.of(
            "warehouse-north-01", new Location(5, 10),
            "warehouse-south-01", new Location(5, 0),
            "warehouse-east-01",  new Location(10, 5),
            "warehouse-west-01",  new Location(0, 5),
            "factory-01",         new Location(0, 0)
    );

    public Location resolve(String warehouseId) {
        Location location = LOCATIONS.get(warehouseId);
        if (location == null) {
            throw new IllegalArgumentException("Unknown warehouse ID: " + warehouseId);
        }
        return location;
    }
}
