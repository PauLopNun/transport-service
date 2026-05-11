package com.gft.transport.delivery.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.delivery.application.usecase.AdvanceTrucks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeAdvancedListenerTest {

    @Mock
    private AdvanceTrucks advanceTrucks;

    private TimeAdvancedListener listener;

    @BeforeEach
    void setUp() {
        listener = new TimeAdvancedListener(advanceTrucks, new ObjectMapper());
    }

    @Test
    void callsAdvanceTrucksWithDaysAdvancedAndCurrentDay() {
        listener.onMessage(buildMessage("{\"previousDayNumber\":1,\"currentDayNumber\":3,\"daysAdvanced\":2}"));

        verify(advanceTrucks).execute(2, 3);
    }

    @Test
    void skipsExecutionWhenDaysAdvancedIsZero() {
        listener.onMessage(buildMessage("{\"previousDayNumber\":1,\"currentDayNumber\":1,\"daysAdvanced\":0}"));

        verifyNoInteractions(advanceTrucks);
    }

    @Test
    void skipsExecutionWhenDaysAdvancedIsNegative() {
        listener.onMessage(buildMessage("{\"previousDayNumber\":2,\"currentDayNumber\":1,\"daysAdvanced\":-1}"));

        verifyNoInteractions(advanceTrucks);
    }

    @Test
    void throwsIllegalArgumentExceptionForInvalidJson() {
        assertThatThrownBy(() -> listener.onMessage(buildMessage("not-json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid time.advanced.v1 message");
    }

    private Message buildMessage(String json) {
        return MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
    }
}
