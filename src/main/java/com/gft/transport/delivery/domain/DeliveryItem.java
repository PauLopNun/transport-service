package com.gft.transport.delivery.domain;

import java.util.UUID;

public record DeliveryItem(UUID productId, int quantity) {}
