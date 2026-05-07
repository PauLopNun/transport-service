package com.gft.transport.delivery.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public record TimeAdvancedMessage(
        UUID eventId,
        int previousDayNumber,
        int currentDayNumber,
        int daysAdvanced,
        Instant occurredAt
) {}
