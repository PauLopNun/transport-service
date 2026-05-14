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
    void calculatesChebyshevDistance() {
        assertThat(calculator.calculate(new Location(0, 0), new Location(3, 4))).isEqualTo(4);
    }

    @Test
    void worksWithNegativeCoordinates() {
        assertThat(calculator.calculate(new Location(-1, -1), new Location(2, 3))).isEqualTo(4);
    }

    @Test
    void isSymmetric() {
        Location a = new Location(1, 2);
        Location b = new Location(4, 6);
        assertThat(calculator.calculate(a, b)).isEqualTo(calculator.calculate(b, a));
    }

    @Test
    void isOnRouteReturnsTrueForPointOnHorizontalSegment() {
        // path: (0,0) → diagonal → (3,3) → straight → (3,4), point (1,1) is on diagonal leg
        assertThat(calculator.isOnRoute(new Location(1, 1), new Location(0, 0), new Location(3, 4))).isTrue();
    }

    @Test
    void isOnRouteReturnsTrueForPointOnVerticalSegment() {
        // path: (0,0) → diagonal → (2,2) → straight → (2,5), point (2,3) is on straight leg
        assertThat(calculator.isOnRoute(new Location(2, 3), new Location(0, 0), new Location(2, 5))).isTrue();
    }

    @Test
    void isOnRouteReturnsTrueForOrigin() {
        assertThat(calculator.isOnRoute(new Location(0, 0), new Location(0, 0), new Location(3, 4))).isTrue();
    }

    @Test
    void isOnRouteReturnsTrueForDestination() {
        assertThat(calculator.isOnRoute(new Location(3, 4), new Location(0, 0), new Location(3, 4))).isTrue();
    }

    @Test
    void isOnRouteReturnsFalseForPointOffPath() {
        // path: (0,0) → diagonal → (3,3) → straight → (3,4), point (2,0) is off path
        assertThat(calculator.isOnRoute(new Location(2, 0), new Location(0, 0), new Location(3, 4))).isFalse();
    }

    @Test
    void isOnRouteWorksWithNegativeCoordinates() {
        // path: (-2,0) → diagonal → (0,2) → straight → (0,3), point (-1,1) is on diagonal leg
        assertThat(calculator.isOnRoute(new Location(-1, 1), new Location(-2, 0), new Location(0, 3))).isTrue();
    }

    @Test
    void isOnRouteReturnsFalseWhenPointIsOnSameYButBeforeXRange() {
        // path: (0,0) → (3,0) → (3,4), point (-1,0) has same Y as from but X is before range
        assertThat(calculator.isOnRoute(new Location(-1, 0), new Location(0, 0), new Location(3, 4))).isFalse();
    }

    @Test
    void isOnRouteReturnsFalseWhenPointIsOnSameXAsDestinationButBeyondYRange() {
        // path: (0,0) → (3,0) → (3,4), point (3,5) shares X with destination but Y is past range
        assertThat(calculator.isOnRoute(new Location(3, 5), new Location(0, 0), new Location(3, 4))).isFalse();
    }

    @Test
    void isOnRouteReturnsTrueForPointOnDiagonalLeg() {
        // path: (0,0) → diagonal → (3,3) → straight → (3,4), point (2,2) is on diagonal leg
        assertThat(calculator.isOnRoute(new Location(2, 2), new Location(0, 0), new Location(3, 4))).isTrue();
    }

    @Test
    void isOnRouteReturnsFalseForPointWithWrongXDirection() {
        // path: (0,0) → diagonal → (3,3) → straight → (3,4)
        // point (-1,1) has wrong X direction for diagonal
        assertThat(calculator.isOnRoute(new Location(-1, 1), new Location(0, 0), new Location(3, 4))).isFalse();
    }

    @Test
    void isOnRouteReturnsFalseForPointWithWrongYDirection() {
        // path: (0,0) → diagonal → (3,3) → straight → (3,4)
        // point (1,-1) has wrong Y direction for diagonal
        assertThat(calculator.isOnRoute(new Location(1, -1), new Location(0, 0), new Location(3, 4))).isFalse();
    }

    @Test
    void isOnRouteReturnsFalseForPointPastDiagonalLeg() {
        // path: (0,0) → diagonal → (3,3) → straight → (3,4)
        // point (4,4) is past the diagonal turning point
        assertThat(calculator.isOnRoute(new Location(4, 4), new Location(0, 0), new Location(3, 4))).isFalse();
    }

    @Test
    void isOnRouteReturnsTrueForPointOnHorizontalStraightLeg() {
        // path: (0,0) → diagonal → (2,2) → straight horizontal to (5,2)
        // point (3,2) is on horizontal straight leg
        assertThat(calculator.isOnRoute(new Location(3, 2), new Location(0, 0), new Location(5, 2))).isTrue();
    }

    @Test
    void isOnRouteReturnsTrueForPointOnVerticalStraightLeg() {
        // path: (0,0) → diagonal → (2,2) → straight vertical to (2,5)
        // point (2,4) is on vertical straight leg
        assertThat(calculator.isOnRoute(new Location(2, 4), new Location(0, 0), new Location(2, 5))).isTrue();
    }

    @Test
    void isOnRouteReturnsTrueForTurningPoint() {
        // Turning point is on both diagonal and straight legs
        assertThat(calculator.isOnRoute(new Location(3, 3), new Location(0, 0), new Location(3, 4))).isTrue();
    }

    @Test
    void isOnRouteReturnsFalseWhenPointNotOnDiagonalDueToUnequalDisplacement() {
        // (2,1) has |dpx|=2, |dpy|=1 - not equal so not on diagonal
        assertThat(calculator.isOnRoute(new Location(2, 1), new Location(0, 0), new Location(3, 3))).isFalse();
    }

    @Test
    void isOnRouteReturnsTrueForPureDiagonalPath() {
        // Pure diagonal: (0,0) → (3,3), point (1,1) on diagonal
        assertThat(calculator.isOnRoute(new Location(1, 1), new Location(0, 0), new Location(3, 3))).isTrue();
    }

    @Test
    void isOnRouteReturnsTrueWhenFromEqualsDestination() {
        // from == to, point at from should be true
        assertThat(calculator.isOnRoute(new Location(5, 5), new Location(5, 5), new Location(5, 5))).isTrue();
    }

    @Test
    void isOnRouteReturnsFalseWhenPointIsNotOnAnyLeg() {
        // (1,3) not on path (0,0)→(2,2)→(2,5)
        assertThat(calculator.isOnRoute(new Location(1, 3), new Location(0, 0), new Location(2, 5))).isFalse();
    }

    @Test
    void isOnRouteReturnsFalseForPointOnHorizontalButOutOfRange() {
        // (5,0) on horizontal but beyond destination x range (0,0)→(3,3)→(3,4)
        assertThat(calculator.isOnRoute(new Location(5, 0), new Location(0, 0), new Location(3, 4))).isFalse();
    }

    @Test
    void isOnRouteReturnsFalseForPointOnVerticalButOutOfRange() {
        // (2,3) has x=2 but straight leg at x=3 for path (0,0)→(2,2)→(2,5)
        assertThat(calculator.isOnRoute(new Location(1, 3), new Location(0, 0), new Location(2, 5))).isFalse();
    }

    @Test
    void isOnRouteReturnsTrueForStartPointOfStraightLeg() {
        // Point at transition from diagonal to straight
        assertThat(calculator.isOnRoute(new Location(2, 2), new Location(0, 0), new Location(2, 5))).isTrue();
    }

    @Test
    void isOnRouteWithNegativeDiagonalPath() {
        // Negative direction: (-3,-3) diagonal with negative values
        assertThat(calculator.isOnRoute(new Location(-2, -2), new Location(0, 0), new Location(-3, -3))).isTrue();
    }

    @Test
    void isOnRouteReturnsFalseForPointOnHorizontalStraightLegButWrongY() {
        // Path: (0,0) → diagonal → (3,3) → straight → (5,3)
        // Point (4,2) is on correct x range but wrong y for horizontal leg
        assertThat(calculator.isOnRoute(new Location(4, 2), new Location(0, 0), new Location(5, 3))).isFalse();
    }

    @Test
    void isOnRouteReturnsFalseForPointOnVerticalStraightLegButWrongX() {
        // Path: (0,0) → diagonal → (2,2) → straight → (2,5)
        // Point (3,4) is on correct y range but wrong x for vertical leg
        assertThat(calculator.isOnRoute(new Location(3, 4), new Location(0, 0), new Location(2, 5))).isFalse();
    }

    @Test
    void isOnRouteReturnsTrueForPointAtEndOfHorizontalStraightLeg() {
        // Point at exact end of straight horizontal leg
        assertThat(calculator.isOnRoute(new Location(5, 3), new Location(0, 0), new Location(5, 3))).isTrue();
    }

    @Test
    void isOnRouteReturnsTrueForPointAtEndOfVerticalStraightLeg() {
        // Point at exact end of straight vertical leg
        assertThat(calculator.isOnRoute(new Location(2, 5), new Location(0, 0), new Location(2, 5))).isTrue();
    }

    @Test
    void isOnRouteDiagonalLegZeroLengthPointOnHorizontalLeg() {
        // Path: (0,0) → (5,0) has zero-length diagonal leg (dy=0)
        // Point (1,0) is on the straight horizontal leg after zero diagonal
        assertThat(calculator.isOnRoute(new Location(1, 0), new Location(0, 0), new Location(5, 0))).isTrue();
    }

    @Test
    void isOnRouteHorizontalStraightLegWithPointNotOnY() {
        // Path: (0,0) → (3,0) straight horizontal, point (2,1) has wrong Y
        assertThat(calculator.isOnRoute(new Location(2, 1), new Location(0, 0), new Location(3, 0))).isFalse();
    }

    @Test
    void isOnRouteHorizontalStraightLegWithPointOutOfXRange() {
        // Path: (0,0) → (3,0) straight horizontal, point (5,0) Y correct but X outside range
        assertThat(calculator.isOnRoute(new Location(5, 0), new Location(0, 0), new Location(3, 0))).isFalse();
    }

    @Test
    void isBetweenBothConditionsMustBeTrue() {
        // Path: (0,0) → (0,2) → (5,2) straight to (5,2), point (5,2) at exact end
        // Tests both branches of && in isBetween: min <= value AND value <= max both true
        assertThat(calculator.isOnRoute(new Location(5, 2), new Location(0, 0), new Location(5, 2))).isTrue();
    }

    @Test
    void isOnRoutePointAtOriginOfZeroDiagonalLeg() {
        // Path: (0,0) → (5,0), zero-length diagonal at (0,0)
        // Point (0,0) equals from, returns true in line 25 branch
        assertThat(calculator.isOnRoute(new Location(0, 0), new Location(0, 0), new Location(5, 0))).isTrue();
    }

    @Test
    void isBetweenShortCircuitWhenMinGreaterThanValue() {
        // Path with point outside range to trigger short-circuit of &&
        // Point (6,0) on path (0,0)→(3,0) has value=6 > max=3
        assertThat(calculator.isOnRoute(new Location(6, 0), new Location(0, 0), new Location(3, 0))).isFalse();
    }
}
