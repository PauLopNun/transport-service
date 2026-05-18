package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;

public class DistanceCalculator {

    public int calculate(Location from, Location to) {
        return Math.max(Math.abs(from.x() - to.x()), Math.abs(from.y() - to.y()));
    }

    public boolean isOnRoute(Location point, Location from, Location to) {
        Location turningPoint = computeTurningPoint(from, to);
        return isOnDiagonalLeg(point, from, turningPoint) || isOnStraightLeg(point, turningPoint, to);
    }

    private Location computeTurningPoint(Location from, Location to) {
        int horizontalDelta = to.x() - from.x();
        int verticalDelta = to.y() - from.y();
        int diagonalSteps = Math.min(Math.abs(horizontalDelta), Math.abs(verticalDelta));
        return new Location(
                from.x() + Integer.signum(horizontalDelta) * diagonalSteps,
                from.y() + Integer.signum(verticalDelta) * diagonalSteps
        );
    }

    private boolean isOnDiagonalLeg(Location point, Location from, Location to) {
        int horizontalDelta = to.x() - from.x();
        int verticalDelta = to.y() - from.y();
        int pointHorizontalDisplacement = point.x() - from.x();
        int pointVerticalDisplacement = point.y() - from.y();
        boolean displacementIsDiagonal = Math.abs(pointHorizontalDisplacement) == Math.abs(pointVerticalDisplacement);
        boolean withinRange = Math.abs(pointHorizontalDisplacement) <= Math.abs(horizontalDelta);
        boolean horizontalDirectionMatches = pointHorizontalDisplacement == 0 || Integer.signum(pointHorizontalDisplacement) == Integer.signum(horizontalDelta);
        boolean verticalDirectionMatches = pointVerticalDisplacement == 0 || Integer.signum(pointVerticalDisplacement) == Integer.signum(verticalDelta);
        return displacementIsDiagonal && withinRange && horizontalDirectionMatches && verticalDirectionMatches;
    }

    private boolean isOnStraightLeg(Location point, Location from, Location to) {
        if (from.equals(to)) return false;
        if (from.x() == to.x()) {
            return point.x() == to.x() && isBetween(point.y(), from.y(), to.y());
        }
        return point.y() == to.y() && isBetween(point.x(), from.x(), to.x());
    }

    private boolean isBetween(int value, int a, int b) {
        return Math.min(a, b) <= value && value <= Math.max(a, b);
    }
}
