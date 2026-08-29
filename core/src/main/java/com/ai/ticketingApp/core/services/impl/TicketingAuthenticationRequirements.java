package com.ai.ticketingApp.core.services.impl;

import org.osgi.service.component.annotations.Component;

@Component(
    service = TicketingAuthenticationRequirements.class,
    property = {
        "sling.auth.requirements=+/content/ticketingApp/us/en/tickets",
        "sling.auth.requirements=+/bin/ticketing"
    }
)
public class TicketingAuthenticationRequirements {
    // The service registration properties extend Sling's authentication requirements.
}
