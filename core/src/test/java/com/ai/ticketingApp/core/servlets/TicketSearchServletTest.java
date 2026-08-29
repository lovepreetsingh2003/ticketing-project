package com.ai.ticketingApp.core.servlets;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.enums.TicketStatus;
import com.ai.ticketingApp.core.services.TicketService;
import com.ai.ticketingApp.core.testcontext.AppAemContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class TicketSearchServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TicketService ticketService;
    private TicketSearchServlet servlet;

    @BeforeEach
    void setUp() {
        ticketService = Mockito.mock(TicketService.class);
        context.registerService(TicketService.class, ticketService);
        servlet = context.registerInjectActivateService(new TicketSearchServlet());
    }

    @Test
    void doGetReturnsMatchingTickets() throws Exception {
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("ticketId", "ticket-001");
        List<Map<String, Object>> tickets = Collections.singletonList(ticket);
        when(ticketService.searchTickets(any(), eq("login"), eq(TicketStatus.OPEN), eq(TicketPriority.HIGH)))
            .thenReturn(tickets);

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "keyword", "login",
            "status", "OPEN",
            "priority", "HIGH"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(200, response.getStatus());
        Map<?, ?> body = MAPPER.readValue(response.getOutputAsString(), Map.class);
        assertEquals(1, body.get("total"));
        List<?> resultTickets = (List<?>) body.get("tickets");
        assertEquals(1, resultTickets.size());
    }

    @Test
    void doGetReturnsServerErrorWhenSearchFails() throws Exception {
        when(ticketService.searchTickets(any(), isNull(), isNull(), isNull()))
            .thenThrow(new RuntimeException("Query failed"));

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(500, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Search failed"));
    }
}
