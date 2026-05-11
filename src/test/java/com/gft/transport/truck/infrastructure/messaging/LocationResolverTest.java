package com.gft.transport.truck.infrastructure.messaging;

import com.gft.transport.truck.domain.Location;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationResolverTest {

    private final LocationResolver resolver = new LocationResolver();

    @Test
    void registersAndResolvesWarehouseLocation() {
        resolver.register("warehouse-north-01", new Location(5, 10));

        Location result = resolver.resolve("warehouse-north-01");

        assertThat(result).isEqualTo(new Location(5, 10));
    }

    @Test
    void throwsExceptionForUnknownWarehouseId() {
        assertThatThrownBy(() -> resolver.resolve("unknown-warehouse"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-warehouse");
    }
}
