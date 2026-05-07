package com.gft.transport.delivery.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimeAdvancedMessage(
        int previousDayNumber,
        int currentDayNumber,
        int daysAdvanced
) {}
