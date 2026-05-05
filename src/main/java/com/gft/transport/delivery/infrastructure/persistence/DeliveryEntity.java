package com.gft.transport.delivery.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryEntity {

    @Id
    private UUID id;
    private UUID shipmentId;
    private UUID truckId;
    private int destX;
    private int destY;
    private int assignedAt;
    private Integer completedAt;

    @ElementCollection
    @CollectionTable(name = "delivery_items", joinColumns = @JoinColumn(name = "delivery_id"))
    private List<DeliveryItemEmbeddable> items;
}