package com.ai.ticketingApp.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusHistoryModelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void constructorRetainsFields() {
        StatusHistoryModel history = new StatusHistoryModel(
            "developer-user", "OPEN", "IN_PROGRESS", 5678L);

        assertEquals("developer-user", history.getUser());
        assertEquals("OPEN", history.getFrom());
        assertEquals("IN_PROGRESS", history.getTo());
        assertEquals(5678L, history.getCreated());
    }

    @Test
    void fromJsonDeserializesStatusHistory() throws Exception {
        String json = "{\"user\":\"qa-user\",\"from\":\"IN_PROGRESS\",\"to\":\"RESOLVED\",\"created\":4321}";

        StatusHistoryModel history = StatusHistoryModel.fromJson(json, MAPPER);

        assertEquals("qa-user", history.getUser());
        assertEquals("IN_PROGRESS", history.getFrom());
        assertEquals("RESOLVED", history.getTo());
        assertEquals(4321L, history.getCreated());
    }
}
