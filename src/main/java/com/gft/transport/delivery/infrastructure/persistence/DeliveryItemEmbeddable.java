package com.gft.transport.delivery.infrastructure.persistence;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryItemEmbeddable {
    @Column(name = "product_id")
    private String productId;

    private int quantity;
}
