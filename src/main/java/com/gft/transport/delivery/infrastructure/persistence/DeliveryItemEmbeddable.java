package com.gft.transport.delivery.infrastructure.persistence;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryItemEmbeddable {
    @Column(name = "product_id")
    private UUID productId;

    private int quantity;
}
