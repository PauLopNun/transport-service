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

    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Column(name = "truck_id")
    private UUID truckId;

    @Column(name = "origin_x")
    private Integer originX;

    @Column(name = "origin_y")
    private Integer originY;

    @Column(name = "dest_x")
    private int destX;

    @Column(name = "dest_y")
    private int destY;

    @Column(name = "assigned_at")
    private int assignedAt;

    @Column(name = "completed_at")
    private Integer completedAt;

    @ElementCollection
    @CollectionTable(name = "delivery_items", joinColumns = @JoinColumn(name = "delivery_id"))
    private List<DeliveryItemEmbeddable> items;
}
