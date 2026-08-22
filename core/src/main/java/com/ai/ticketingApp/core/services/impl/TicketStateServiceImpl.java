package com.ai.ticketingApp.core.services.impl;

import com.ai.ticketingApp.core.enums.TicketStatus;
import com.ai.ticketingApp.core.services.TicketStateService;
import org.osgi.service.component.annotations.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component(service = TicketStateService.class)
public class TicketStateServiceImpl implements TicketStateService {

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS;

    static {
        Map<TicketStatus, Set<TicketStatus>> map = new EnumMap<>(TicketStatus.class);
        map.put(TicketStatus.OPEN,        EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
        map.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED,    TicketStatus.CANCELLED));
        map.put(TicketStatus.RESOLVED,    EnumSet.of(TicketStatus.CLOSED));
        map.put(TicketStatus.CLOSED,      Collections.emptySet());
        map.put(TicketStatus.CANCELLED,   Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    @Override
    public boolean isTransitionAllowed(TicketStatus current, TicketStatus next) {
        Set<TicketStatus> allowed = TRANSITIONS.getOrDefault(current, Collections.emptySet());
        return allowed.contains(next);
    }

    @Override
    public Set<TicketStatus> getAllowedTransitions(TicketStatus current) {
        return TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }

    @Override
    public void assertTransitionAllowed(TicketStatus current, TicketStatus next) {
        if (!isTransitionAllowed(current, next)) {
            throw new IllegalStateException(
                "Cannot transition from " + current.name() + " to " + next.name());
        }
    }
}
