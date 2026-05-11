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

import static org.assertj.core.api.Assertions.assertThat;
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
        listener.onMessage(buildMessage("{\"previousDay\":1,\"currentDay\":3,\"daysAdvanced\":2}"));

        verify(advanceTrucks).execute(2, 3);
    }

    @Test
    void skipsExecutionWhenDaysAdvancedIsZero() {
        listener.onMessage(buildMessage("{\"previousDay\":1,\"currentDay\":1,\"daysAdvanced\":0}"));

        verifyNoInteractions(advanceTrucks);
    }

    @Test
    void skipsExecutionWhenDaysAdvancedIsNegative() {
        listener.onMessage(buildMessage("{\"previousDay\":2,\"currentDay\":1,\"daysAdvanced\":-1}"));

        verifyNoInteractions(advanceTrucks);
    }

    @Test
    void deserializesAllFieldsFromRubensFormat() throws Exception {
        String json = "{\"previousDay\":1,\"currentDay\":3,\"daysAdvanced\":2}";
        TimeAdvancedMessage msg = new ObjectMapper().readValue(json, TimeAdvancedMessage.class);

        assertThat(msg.previousDayNumber()).isEqualTo(1);
        assertThat(msg.currentDayNumber()).isEqualTo(3);
        assertThat(msg.daysAdvanced()).isEqualTo(2);
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
