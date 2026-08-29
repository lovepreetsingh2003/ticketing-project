package com.ai.ticketingApp.core.models;

import com.ai.ticketingApp.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class TicketListModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(TicketModel.class, TicketListModel.class);
    }

    @Test
    void loadsTicketsFromTicketsRoot() {
        context.build().resource("/content/ticketingApp/tickets");
        context.build().resource("/content/ticketingApp/tickets/ticket-001",
            "ticketId", "ticket-001",
            "title", "First ticket",
            "status", "OPEN"
        );
        context.build().resource("/content/ticketingApp/tickets/ticket-002",
            "ticketId", "ticket-002",
            "title", "Second ticket",
            "status", "IN_PROGRESS"
        );
        context.build().resource("/content/ticketingApp/tickets/not-a-ticket",
            "title", "Missing ticketId"
        ).commit();

        SlingHttpServletRequest request = context.request();
        TicketListModel model = request.adaptTo(TicketListModel.class);

        assertNotNull(model);
        assertTrue(model.isHasTickets());
        assertEquals(2, model.getTickets().size());
        assertEquals("ticket-001", model.getTickets().get(0).getTicketId());
        assertEquals("ticket-002", model.getTickets().get(1).getTicketId());
    }

    @Test
    void returnsEmptyListWhenTicketsRootMissing() {
        SlingHttpServletRequest request = context.request();
        TicketListModel model = request.adaptTo(TicketListModel.class);

        assertNotNull(model);
        assertFalse(model.isHasTickets());
        assertTrue(model.getTickets().isEmpty());
    }
}
