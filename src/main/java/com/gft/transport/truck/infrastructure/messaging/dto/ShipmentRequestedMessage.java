package com.gft.transport.truck.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gft.transport.delivery.domain.DeliveryItem;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentRequestedMessage(
        UUID shipmentId,
        String originId,
        String destinationId,
        List<DeliveryItem> items,
        int requestedAt
) {}
