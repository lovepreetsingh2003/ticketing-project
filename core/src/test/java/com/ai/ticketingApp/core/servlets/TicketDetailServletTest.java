package com.ai.ticketingApp.core.servlets;

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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class TicketDetailServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TicketService ticketService;
    private TicketDetailServlet servlet;

    @BeforeEach
    void setUp() {
        ticketService = Mockito.mock(TicketService.class);
        context.registerService(TicketService.class, ticketService);
        servlet = context.registerInjectActivateService(new TicketDetailServlet());
    }

    @Test
    void doGetReturnsTicketWhenFound() throws Exception {
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("ticketId", "ticket-001");
        ticket.put("title", "Sample ticket");
        when(ticketService.getTicket(any(), eq("/content/ticketingApp/tickets/ticket-001")))
            .thenReturn(ticket);

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(200, response.getStatus());
        Map<?, ?> body = MAPPER.readValue(response.getOutputAsString(), Map.class);
        assertEquals("ticket-001", body.get("ticketId"));
        assertEquals("Sample ticket", body.get("title"));
    }

    @Test
    void doGetRequiresTicketPath() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("ticketPath parameter is required"));
    }

    @Test
    void doGetReturnsNotFoundWhenTicketMissing() throws Exception {
        when(ticketService.getTicket(any(), eq("/content/ticketingApp/tickets/missing")))
            .thenReturn(null);

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/missing"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(404, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Ticket not found"));
    }
}
