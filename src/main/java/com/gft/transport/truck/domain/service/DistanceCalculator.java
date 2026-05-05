package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;

public class DistanceCalculator {

    public int calculate(Location from, Location to) {
        return Math.abs(to.x() - from.x()) + Math.abs(to.y() - from.y());
    }

    public boolean isOnRoute(Location point, Location from, Location to) {
        // Manhattan movement: X first, then Y
        // Horizontal leg: y == from.y, x between from.x and to.x
        boolean onHorizontal = point.y() == from.y()
                && isBetween(point.x(), from.x(), to.x());
        // Vertical leg: x == to.x, y between from.y and to.y
        boolean onVertical = point.x() == to.x()
                && isBetween(point.y(), from.y(), to.y());
        return onHorizontal || onVertical;
    }

    private boolean isBetween(int value, int a, int b) {
        return Math.min(a, b) <= value && value <= Math.max(a, b);
    }
}