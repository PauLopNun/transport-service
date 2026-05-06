package com.gft.transport.truck.application.usecase;

import com.gft.transport.delivery.domain.DeliveryItem;
import com.gft.transport.truck.domain.Location;

import java.util.List;
import java.util.UUID;

public record AssignTruckCommand(
        UUID shipmentId,
        Location origin,
        Location destination,
        List<DeliveryItem> items,
        int requestedAt
) {}
