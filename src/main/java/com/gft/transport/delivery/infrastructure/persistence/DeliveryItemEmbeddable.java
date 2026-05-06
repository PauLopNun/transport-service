package com.gft.transport.delivery.infrastructure.persistence;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryItemEmbeddable {
    private String materialType;
    private int quantity;
}
