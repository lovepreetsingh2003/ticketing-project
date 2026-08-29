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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class TicketCommentServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TicketService ticketService;
    private TicketCommentServlet servlet;

    @BeforeEach
    void setUp() {
        ticketService = Mockito.mock(TicketService.class);
        context.registerService(TicketService.class, ticketService);
        servlet = context.registerInjectActivateService(new TicketCommentServlet());
    }

    @Test
    void doPostAddsCommentWhenValid() throws Exception {
        String author = context.resourceResolver().getUserID();
        when(ticketService.addComment(any(),
            eq("/content/ticketingApp/tickets/ticket-001"), eq(author), eq("Looks good")))
            .thenReturn("/content/ticketingApp/tickets/ticket-001");

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001",
            "body", "Looks good"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(201, response.getStatus());
        Map<?, ?> body = MAPPER.readValue(response.getOutputAsString(), Map.class);
        assertEquals(true, body.get("success"));
        assertEquals("/content/ticketingApp/tickets/ticket-001", body.get("ticketPath"));
        assertEquals(author, body.get("author"));
        verify(ticketService).addComment(any(),
            eq("/content/ticketingApp/tickets/ticket-001"), eq(author), eq("Looks good"));
    }

    @Test
    void doPostRequiresTicketPath() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of("body", "Comment text"));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("ticketPath parameter is required"));
    }

    @Test
    void doPostRequiresCommentBody() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/ticket-001"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Comment body is required"));
    }

    @Test
    void doPostReturnsNotFoundForMissingTicket() throws Exception {
        when(ticketService.addComment(any(), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Ticket not found"));

        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Map.of(
            "ticketPath", "/content/ticketingApp/tickets/missing",
            "body", "Comment"
        ));
        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request, response);

        assertEquals(404, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Ticket not found"));
    }
}
