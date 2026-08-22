package com.ai.ticketingApp.core.services;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.enums.TicketStatus;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;
import java.util.Map;

public interface TicketService {

    /**
     * Creates a new ticket and returns its node path.
     */
    String createTicket(ResourceResolver resolver, String title, String description,
                        TicketPriority priority, String assignee, String reporter);

    /**
     * Updates editable fields on an existing ticket.
     */
    void updateTicket(ResourceResolver resolver, String ticketPath, String title,
                      String description, TicketPriority priority, String assignee);

    /**
     * Changes the status of a ticket. Throws IllegalStateException for invalid transitions.
     */
    void changeStatus(ResourceResolver resolver, String ticketPath, TicketStatus newStatus);

    /**
     * Adds a comment to a ticket and returns the comment node path.
     */
    String addComment(ResourceResolver resolver, String ticketPath, String author, String body);

    /**
     * Returns a single ticket as a property map, or null if not found.
     */
    Map<String, Object> getTicket(ResourceResolver resolver, String ticketPath);

    /**
     * Returns all tickets matching optional keyword and status/priority filters.
     */
    List<Map<String, Object>> searchTickets(ResourceResolver resolver, String keyword,
                                            TicketStatus statusFilter, TicketPriority priorityFilter);
}
