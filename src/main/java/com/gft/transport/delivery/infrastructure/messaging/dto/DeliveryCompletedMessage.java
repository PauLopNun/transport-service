package com.gft.transport.delivery.infrastructure.messaging.dto;

import com.gft.transport.delivery.domain.event.DeliveryCompletedEvent;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
/**
 * Trello: TRK-43 / TRK-251 - Publish delivery.completed.v1.
 */
public class DeliveryCompletedMessage {

    String shipmentId;
    String truckId;
    List<ItemDto> items;
    LocationDto location;
    int completedAt;

    @Value
    public static class ItemDto {
        String materialType;
        int quantity;
    }

    @Value
    public static class LocationDto {
        int x;
        int y;
    }

    public static DeliveryCompletedMessage from(DeliveryCompletedEvent event) {
        return DeliveryCompletedMessage.builder()
                .shipmentId(event.getShipmentId().toString())
                .truckId(event.getTruckId().value().toString())
                .items(event.getItems().stream()
                        .map(item -> new ItemDto(item.materialType(), item.quantity()))
                        .toList())
                .location(new LocationDto(event.getLocation().x(), event.getLocation().y()))
                .completedAt(event.getCompletedAt())
                .build();
    }
}
