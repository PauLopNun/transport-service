package com.gft.transport.delivery.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimeAdvancedMessage(
        @JsonProperty("previousDay") int previousDayNumber,
        @JsonProperty("currentDay") int currentDayNumber,
        int daysAdvanced
) {}
