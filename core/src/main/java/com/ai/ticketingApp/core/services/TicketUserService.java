package com.ai.ticketingApp.core.services;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;
import java.util.Map;

public interface TicketUserService {

    List<Map<String, String>> getAssignableUsers(ResourceResolver resolver);

    boolean isAssignableUser(ResourceResolver resolver, String userId);
}
