package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceCalculatorTest {

    private DistanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DistanceCalculator();
    }

    @Test
    void returnsZeroWhenBothLocationsAreTheSame() {
        assertThat(calculator.calculate(new Location(0, 0), new Location(0, 0))).isEqualTo(0);
    }

    @Test
    void calculatesHorizontalDistance() {
        assertThat(calculator.calculate(new Location(0, 0), new Location(3, 0))).isEqualTo(3);
    }

    @Test
    void calculatesVerticalDistance() {
        assertThat(calculator.calculate(new Location(0, 0), new Location(0, 4))).isEqualTo(4);
    }

    @Test
    void calculatesManhattanDistanceNotEuclidean() {
        assertThat(calculator.calculate(new Location(0, 0), new Location(3, 4))).isEqualTo(7);
    }

    @Test
    void worksWithNegativeCoordinates() {
        assertThat(calculator.calculate(new Location(-1, -1), new Location(2, 3))).isEqualTo(7);
    }

    @Test
    void isSymmetric() {
        Location a = new Location(1, 2);
        Location b = new Location(4, 6);
        assertThat(calculator.calculate(a, b)).isEqualTo(calculator.calculate(b, a));
    }
}
