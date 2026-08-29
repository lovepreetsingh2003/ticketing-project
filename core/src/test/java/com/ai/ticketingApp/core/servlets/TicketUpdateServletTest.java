package com.ai.ticketingApp.core.servlets;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.services.TicketService;
import com.ai.ticketingApp.core.services.TicketUserService;
import com.ai.ticketingApp.core.services.TicketValidationService;
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
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class TicketUpdateServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TicketService ticketService;
    private TicketValidationService validationService;
    private TicketUserService ticketUserService;
    private TicketUpdateServlet servlet;

    @BeforeEach
    void setUp() {
        ticketService = Mockito.mock(TicketService.class);
        validationService = Mockito.mock(TicketValidationService.class);
        ticketUserService = Mockito.mock(TicketUserService.class);

        context.registerService(TicketService.class, ticketService);
        context.registerService(TicketValidationService.class, validationService);
        context.registerService(TicketUserService.class, ticketUserService);
        servlet = context.registerInjectActivateService(new TicketUpdateServlet());
    }

    @Test
    void doPostUpdatesTicketWhenValid() throws Exception {
        when(validationService.validateUpdate("New title", "New desc", "LOW")).thenReturn(null);
        when(ticketUserService.isAssignableUser(any(), eq("qa-user"))).thenReturn(true);

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001",
            "title", "New title",
            "description", "New desc",
            "priority", "LOW",
            "assignee", "qa-user"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(200, response.getStatus());
        Map<?, ?> body = MAPPER.readValue(response.getOutputAsString(), Map.class);
        assertEquals(true, body.get("success"));
        verify(ticketService).updateTicket(any(),
            eq("/content/ticketingApp/tickets/ticket-001"),
            eq("New title"), eq("New desc"), eq(TicketPriority.LOW), eq("qa-user"));
    }

    @Test
    void doPostRequiresTicketPath() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("ticketPath parameter is required"));
    }

    @Test
    void doPostReturnsNotFoundForMissingTicket() throws Exception {
        when(validationService.validateUpdate(any(), any(), any())).thenReturn(null);
        when(ticketUserService.isAssignableUser(any(), any())).thenReturn(true);
        doThrow(new IllegalArgumentException("Ticket not found"))
            .when(ticketService).updateTicket(any(), any(), any(), any(), any(), any());

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/missing"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(404, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Ticket not found"));
    }
}
