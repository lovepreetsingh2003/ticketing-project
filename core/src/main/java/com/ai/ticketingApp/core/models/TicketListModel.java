package com.ai.ticketingApp.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Model(adaptables = {SlingHttpServletRequest.class, Resource.class},
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TicketListModel {

    private static final String TICKETS_ROOT = "/content/ticketingApp/tickets";

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<TicketModel> tickets = new ArrayList<>();

    @PostConstruct
    protected void init() {
        Resource ticketsRoot = resourceResolver.getResource(TICKETS_ROOT);
        if (ticketsRoot != null) {
            for (Resource ticketResource : ticketsRoot.getChildren()) {
                if (ticketResource.getValueMap().containsKey("ticketId")) {
                    TicketModel model = ticketResource.adaptTo(TicketModel.class);
                    if (model != null) {
                        tickets.add(model);
                    }
                }
            }
        }
    }

    public List<TicketModel> getTickets() {
        return tickets;
    }

    public boolean isHasTickets() {
        return !tickets.isEmpty();
    }
}
