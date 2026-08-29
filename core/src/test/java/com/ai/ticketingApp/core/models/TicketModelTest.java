package com.ai.ticketingApp.core.models;

import com.ai.ticketingApp.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class TicketModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(TicketModel.class);
    }

    @Test
    void adaptsFromResourceWithAllFields() {
        Calendar created = Calendar.getInstance();
        Calendar updated = Calendar.getInstance();

        context.build().resource("/content/ticketingApp/tickets/ticket-001",
            "ticketId", "ticket-001",
            "title", "Login page broken",
            "description", "Users cannot sign in",
            "priority", "HIGH",
            "status", "OPEN",
            "assignee", "dev-user",
            "reporter", "qa-user",
            "created", created,
            "updated", updated,
            "comments", new String[] {
                "{\"author\":\"qa-user\",\"body\":\"Reproduced\",\"created\":1000}"
            },
            "statusHistory", new String[] {
                "{\"user\":\"qa-user\",\"from\":\"OPEN\",\"to\":\"IN_PROGRESS\",\"created\":2000}"
            }
        ).commit();

        Resource resource = context.resourceResolver().getResource("/content/ticketingApp/tickets/ticket-001");
        TicketModel model = resource.adaptTo(TicketModel.class);

        assertNotNull(model);
        assertEquals("ticket-001", model.getTicketId());
        assertEquals("Login page broken", model.getTitle());
        assertEquals("Users cannot sign in", model.getDescription());
        assertEquals("HIGH", model.getPriority());
        assertEquals("OPEN", model.getStatus());
        assertEquals("dev-user", model.getAssignee());
        assertEquals("qa-user", model.getReporter());
        assertEquals(created, model.getCreated());
        assertEquals(updated, model.getUpdated());
        assertEquals("/content/ticketingApp/tickets/ticket-001", model.getPath());
        assertFalse(model.isTerminal());

        List<String> transitions = model.getAllowedTransitions();
        assertEquals(2, transitions.size());
        assertTrue(transitions.contains("IN_PROGRESS"));
        assertTrue(transitions.contains("CANCELLED"));

        assertEquals(1, model.getComments().size());
        assertEquals("qa-user", model.getComments().get(0).getAuthor());
        assertEquals("Reproduced", model.getComments().get(0).getBody());
        assertEquals(1000L, model.getComments().get(0).getCreated());

        assertEquals(1, model.getStatusHistory().size());
        assertEquals("qa-user", model.getStatusHistory().get(0).getUser());
        assertEquals("OPEN", model.getStatusHistory().get(0).getFrom());
        assertEquals("IN_PROGRESS", model.getStatusHistory().get(0).getTo());
        assertEquals(2000L, model.getStatusHistory().get(0).getCreated());
    }

    @Test
    void resolvedStatusAllowsClosedTransition() {
        context.build().resource("/content/ticketingApp/tickets/ticket-002",
            "ticketId", "ticket-002",
            "status", "RESOLVED"
        ).commit();

        TicketModel model = context.resourceResolver()
            .getResource("/content/ticketingApp/tickets/ticket-002")
            .adaptTo(TicketModel.class);

        assertNotNull(model);
        assertEquals(List.of("CLOSED"), model.getAllowedTransitions());
        assertFalse(model.isTerminal());
    }

    @Test
    void closedAndCancelledAreTerminal() {
        context.build().resource("/content/ticketingApp/tickets/ticket-closed",
            "ticketId", "ticket-closed",
            "status", "CLOSED"
        ).commit();
        context.build().resource("/content/ticketingApp/tickets/ticket-cancelled",
            "ticketId", "ticket-cancelled",
            "status", "CANCELLED"
        ).commit();

        TicketModel closed = context.resourceResolver()
            .getResource("/content/ticketingApp/tickets/ticket-closed")
            .adaptTo(TicketModel.class);
        TicketModel cancelled = context.resourceResolver()
            .getResource("/content/ticketingApp/tickets/ticket-cancelled")
            .adaptTo(TicketModel.class);

        assertTrue(closed.isTerminal());
        assertTrue(closed.getAllowedTransitions().isEmpty());
        assertTrue(cancelled.isTerminal());
        assertTrue(cancelled.getAllowedTransitions().isEmpty());
    }

    @Test
    void ignoresMalformedAuditEntries() {
        context.build().resource("/content/ticketingApp/tickets/ticket-003",
            "ticketId", "ticket-003",
            "status", "OPEN",
            "comments", new String[] { "not-json" },
            "statusHistory", new String[] { "{invalid}" }
        ).commit();

        TicketModel model = context.resourceResolver()
            .getResource("/content/ticketingApp/tickets/ticket-003")
            .adaptTo(TicketModel.class);

        assertNotNull(model);
        assertTrue(model.getComments().isEmpty());
        assertTrue(model.getStatusHistory().isEmpty());
    }
}
