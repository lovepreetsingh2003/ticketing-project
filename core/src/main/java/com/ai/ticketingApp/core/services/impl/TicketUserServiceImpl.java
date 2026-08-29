package com.ai.ticketingApp.core.services.impl;

import com.ai.ticketingApp.core.services.TicketUserService;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component(service = TicketUserService.class)
public class TicketUserServiceImpl implements TicketUserService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketUserServiceImpl.class);
    private static final String SUBSERVICE_NAME = "ticketing-user-reader";
    private static final String[] ASSIGNEE_GROUPS = {"ticketing-qa", "ticketing-developers"};

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public List<Map<String, String>> getAssignableUsers(ResourceResolver resolver) {
        try (ResourceResolver serviceResolver = getServiceResourceResolver()) {
            if (serviceResolver == null) {
                return new ArrayList<>();
            }
            return loadAssignableUsers(serviceResolver);
        }
    }

    @Override
    public boolean isAssignableUser(ResourceResolver resolver, String userId) {
        for (Map<String, String> user : getAssignableUsers(resolver)) {
            if (userId != null && userId.equals(user.get("userId"))) {
                return true;
            }
        }
        return false;
    }

    private ResourceResolver getServiceResourceResolver() {
        Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME);
        try {
            return resourceResolverFactory.getServiceResourceResolver(authInfo);
        } catch (LoginException e) {
            LOG.error("Unable to obtain service resource resolver for assignable users", e);
            return null;
        }
    }

    private List<Map<String, String>> loadAssignableUsers(ResourceResolver resolver) {
        Map<String, Map<String, String>> usersById = new TreeMap<>();
        UserManager userManager = resolver.adaptTo(UserManager.class);
        if (userManager == null) {
            return new ArrayList<>();
        }

        try {
            for (String groupId : ASSIGNEE_GROUPS) {
                addGroupMembers(userManager, groupId, usersById);
            }
        } catch (RepositoryException e) {
            LOG.error("Unable to load assignable ticket users", e);
            return new ArrayList<>();
        }
        return new ArrayList<>(usersById.values());
    }

    private void addGroupMembers(UserManager userManager, String groupId,
                                 Map<String, Map<String, String>> usersById)
            throws RepositoryException {
        Authorizable authorizable = userManager.getAuthorizable(groupId);
        if (authorizable == null || !authorizable.isGroup()) {
            return;
        }

        Iterator<Authorizable> members = ((Group) authorizable).getMembers();
        while (members.hasNext()) {
            Authorizable member = members.next();
            if (member.isGroup()) {
                continue;
            }
            String userId = member.getID();
            Map<String, String> user = new HashMap<>();
            user.put("userId", userId);
            user.put("displayName", userId);
            usersById.put(userId, user);
        }
    }
}
