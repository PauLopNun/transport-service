package com.gft.transport.truck.infrastructure.persistence;

import com.gft.transport.truck.domain.TruckStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trucks")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckEntity {

    @Id
    private UUID id;

    private String name;
    private int x;
    private int y;

    @Enumerated(EnumType.STRING)
    private TruckStatus status;

    private int capacity;

    @Column(name = "current_load")
    private int currentLoad;

    @ElementCollection
    @CollectionTable(name = "truck_deliveries", joinColumns = @JoinColumn(name = "truck_id"))
    @Column(name = "delivery_id")
    private List<UUID> deliveryIds;

    @Column(name = "pending_deletion", nullable = false)
    private boolean pendingDeletion;
}
