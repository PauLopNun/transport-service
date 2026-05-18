package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;

public class DistanceCalculator {

    public int calculate(Location from, Location to) {
        return Math.max(Math.abs(from.x() - to.x()), Math.abs(from.y() - to.y()));
    }

    public boolean isOnRoute(Location point, Location from, Location to) {
        int horizontalDelta = to.x() - from.x();
        int verticalDelta = to.y() - from.y();
        int diagonalSteps = Math.min(Math.abs(horizontalDelta), Math.abs(verticalDelta));
        Location turningPoint = new Location(
                from.x() + Integer.signum(horizontalDelta) * diagonalSteps,
                from.y() + Integer.signum(verticalDelta) * diagonalSteps
        );
        return isOnDiagonalLeg(point, from, turningPoint) || isOnStraightLeg(point, turningPoint, to);
    }

    private boolean isOnDiagonalLeg(Location point, Location from, Location to) {
        int horizontalDelta = to.x() - from.x();
        int verticalDelta = to.y() - from.y();
        if ((horizontalDelta == 0) && (verticalDelta == 0)) return point.equals(from);
        int pointHorizontalDisplacement = point.x() - from.x();
        int pointVerticalDisplacement = point.y() - from.y();
        if (Math.abs(pointHorizontalDisplacement) != Math.abs(pointVerticalDisplacement)) return false;
        if (Math.abs(pointHorizontalDisplacement) > Math.abs(horizontalDelta)) return false;
        if (pointHorizontalDisplacement != 0 && Integer.signum(pointHorizontalDisplacement) != Integer.signum(horizontalDelta)) return false;
        if (pointVerticalDisplacement != 0 && Integer.signum(pointVerticalDisplacement) != Integer.signum(verticalDelta)) return false;
        return true;
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
