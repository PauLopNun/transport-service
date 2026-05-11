package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;

public class DistanceCalculator {

    public int calculate(Location from, Location to) {
        return Math.abs(to.x() - from.x()) + Math.abs(to.y() - from.y());
    }

    public boolean isOnRoute(Location point, Location from, Location to) {
        boolean onHorizontalLeg = point.y() == from.y() && isBetween(point.x(), from.x(), to.x());
        boolean onVerticalLeg = point.x() == to.x() && isBetween(point.y(), from.y(), to.y());
        return onHorizontalLeg || onVerticalLeg;
    }

    private boolean isBetween(int value, int a, int b) {
        return Math.min(a, b) <= value && value <= Math.max(a, b);
    }
}
