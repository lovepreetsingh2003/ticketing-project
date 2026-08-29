package com.ai.ticketingApp.core.services;

import com.ai.ticketingApp.core.enums.TicketStatus;

import java.util.Set;

public interface TicketStateService {

    /**
     * Returns true if transitioning from {@code current} to {@code next} is allowed.
     */
    boolean isTransitionAllowed(TicketStatus current, TicketStatus next);

    /**
     * Returns the set of valid next statuses for the given current status.
     */
    Set<TicketStatus> getAllowedTransitions(TicketStatus current);

    /**
     * Asserts the transition is valid. Throws IllegalStateException with a descriptive message if not.
     */
    void assertTransitionAllowed(TicketStatus current, TicketStatus next);
}
