package com.gft.transport.truck.domain.service;

import com.gft.transport.truck.domain.Location;

public class DistanceCalculator {

    public int calculate(Location from, Location to) {
        return Math.abs(to.x() - from.x()) + Math.abs(to.y() - from.y());
    }
}