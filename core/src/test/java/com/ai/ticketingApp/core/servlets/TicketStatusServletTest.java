package com.ai.ticketingApp.core.servlets;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(AemContextExtension.class)
class TicketStatusServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TicketService ticketService;
    private TicketStatusServlet servlet;

    @BeforeEach
    void setUp() {
        ticketService = Mockito.mock(TicketService.class);
        context.registerService(TicketService.class, ticketService);
        servlet = context.registerInjectActivateService(new TicketStatusServlet());
    }

    @Test
    void doPostChangesStatusWhenValid() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001",
            "status", "IN_PROGRESS"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(200, response.getStatus());
        Map<?, ?> body = MAPPER.readValue(response.getOutputAsString(), Map.class);
        assertEquals(true, body.get("success"));
        assertEquals("IN_PROGRESS", body.get("status"));
        verify(ticketService).changeStatus(any(),
            eq("/content/ticketingApp/tickets/ticket-001"), eq(TicketStatus.IN_PROGRESS));
    }

    @Test
    void doPostRequiresTicketPath() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of("status", "OPEN"));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("ticketPath parameter is required"));
    }

    @Test
    void doPostRequiresStatusParameter() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("status parameter is required"));
    }

    @Test
    void doPostRejectsInvalidStatus() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001",
            "status", "INVALID"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Invalid status value"));
    }

    @Test
    void doPostReturnsConflictForInvalidTransition() throws Exception {
        doThrow(new IllegalStateException("Invalid transition"))
            .when(ticketService).changeStatus(any(), any(), any());

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001",
            "status", "CLOSED"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(409, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Invalid transition"));
    }
}
