package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;

public class DistanceCalculator {

    public int calculate(Location from, Location to) {
        return Math.max(Math.abs(from.x() - to.x()), Math.abs(from.y() - to.y()));
    }

    public boolean isOnRoute(Location point, Location from, Location to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        int diagonalSteps = Math.min(Math.abs(dx), Math.abs(dy));
        Location turningPoint = new Location(
                from.x() + Integer.signum(dx) * diagonalSteps,
                from.y() + Integer.signum(dy) * diagonalSteps
        );
        return isOnDiagonalLeg(point, from, turningPoint) || isOnStraightLeg(point, turningPoint, to);
    }

    private boolean isOnDiagonalLeg(Location point, Location from, Location to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if ((dx == 0) & (dy == 0)) return point.equals(from);
        int dpx = point.x() - from.x();
        int dpy = point.y() - from.y();
        if (Math.abs(dpx) != Math.abs(dpy)) return false;
        if (Math.abs(dpx) > Math.abs(dx)) return false;
        if (dpx != 0 && Integer.signum(dpx) != Integer.signum(dx)) return false;
        if (dpy != 0 && Integer.signum(dpy) != Integer.signum(dy)) return false;
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
