package com.gft.transport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CoverageMarkerTest {
    @Test
    void coversAllMethods() {
        CoverageMarker marker = new CoverageMarker();
        Assertions.assertEquals(1, marker.value());
        Assertions.assertEquals("ok", marker.status());
    }
}

