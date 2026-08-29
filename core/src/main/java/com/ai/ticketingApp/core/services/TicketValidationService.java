package com.ai.ticketingApp.core.services;

import com.ai.ticketingApp.core.enums.TicketPriority;

public interface TicketValidationService {

    /**
     * Validates ticket creation fields. Returns an error message or null if valid.
     */
    String validateCreate(String title, String description, String priority, String assignee, String reporter);

    /**
     * Validates ticket update fields. Returns an error message or null if valid.
     */
    String validateUpdate(String title, String description, String priority);
}
