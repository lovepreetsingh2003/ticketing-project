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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class TicketCreateServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TicketService ticketService;
    private TicketValidationService validationService;
    private TicketUserService ticketUserService;
    private TicketCreateServlet servlet;

    @BeforeEach
    void setUp() {
        ticketService = Mockito.mock(TicketService.class);
        validationService = Mockito.mock(TicketValidationService.class);
        ticketUserService = Mockito.mock(TicketUserService.class);

        context.registerService(TicketService.class, ticketService);
        context.registerService(TicketValidationService.class, validationService);
        context.registerService(TicketUserService.class, ticketUserService);
        servlet = context.registerInjectActivateService(new TicketCreateServlet());
    }

    @Test
    void doPostCreatesTicketWhenValid() throws Exception {
        String reporter = context.resourceResolver().getUserID();
        when(validationService.validateCreate("Bug", "Details", "HIGH", "dev-user", reporter))
            .thenReturn(null);
        when(ticketUserService.isAssignableUser(any(), eq("dev-user"))).thenReturn(true);
        when(ticketService.createTicket(any(), eq("Bug"), eq("Details"),
            eq(TicketPriority.HIGH), eq("dev-user"), eq(reporter)))
            .thenReturn("/content/ticketingApp/tickets/ticket-001");

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "title", "Bug",
            "description", "Details",
            "priority", "HIGH",
            "assignee", "dev-user"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(201, response.getStatus());
        Map<?, ?> body = MAPPER.readValue(response.getOutputAsString(), Map.class);
        assertEquals(true, body.get("success"));
        assertEquals("/content/ticketingApp/tickets/ticket-001", body.get("path"));
        assertEquals("ticket-001", body.get("ticketId"));
        verify(ticketService).createTicket(any(), eq("Bug"), eq("Details"),
            eq(TicketPriority.HIGH), eq("dev-user"), eq(reporter));
    }

    @Test
    void doPostReturnsBadRequestOnValidationError() throws Exception {
        when(validationService.validateCreate(any(), any(), any(), any(), any()))
            .thenReturn("Title is required");

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of("title", ""));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        Map<?, ?> body = MAPPER.readValue(response.getOutputAsString(), Map.class);
        assertEquals("Title is required", body.get("error"));
    }

    @Test
    void doPostReturnsBadRequestForNonAssignableUser() throws Exception {
        when(validationService.validateCreate(any(), any(), any(), any(), any())).thenReturn(null);
        when(ticketUserService.isAssignableUser(any(), eq("invalid-user"))).thenReturn(false);

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of("assignee", "invalid-user"));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Assignee must be a QA or Developer user"));
    }

    @Test
    void doPostReturnsServerErrorWhenCreateFails() throws Exception {
        when(validationService.validateCreate(any(), any(), any(), any(), any())).thenReturn(null);
        when(ticketUserService.isAssignableUser(any(), any())).thenReturn(true);
        when(ticketService.createTicket(any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("JCR error"));

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of("assignee", "dev-user"));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(500, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Failed to create ticket"));
    }
}
