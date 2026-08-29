package com.ai.ticketingApp.core.services.impl;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.enums.TicketStatus;
import com.ai.ticketingApp.core.services.TicketService;
import com.ai.ticketingApp.core.services.TicketStateService;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component(service = TicketService.class)
public class TicketServiceImpl implements TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketServiceImpl.class);
    private static final String TICKETS_ROOT = "/content/ticketingApp/tickets";
    private static final String NT_UNSTRUCTURED = "nt:unstructured";

    @Reference
    private TicketStateService stateService;

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public String createTicket(ResourceResolver resolver, String title, String description,
                               TicketPriority priority, String assignee, String reporter) {
        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new IllegalStateException("Cannot obtain JCR session");
            }
            Node ticketsRoot = JcrUtils.getOrCreateByPath(TICKETS_ROOT, NT_UNSTRUCTURED, session);
            String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Node ticketNode = ticketsRoot.addNode(ticketId, NT_UNSTRUCTURED);
            ticketNode.setProperty("ticketId", ticketId);
            ticketNode.setProperty("title", title);
            ticketNode.setProperty("description", description);
            ticketNode.setProperty("priority", priority.name());
            ticketNode.setProperty("status", TicketStatus.OPEN.name());
            ticketNode.setProperty("assignee", StringUtils.defaultString(assignee));
            ticketNode.setProperty("reporter", StringUtils.defaultString(reporter));
            Calendar now = Calendar.getInstance();
            ticketNode.setProperty("created", now);
            ticketNode.setProperty("updated", now);
            ticketNode.addNode("comments", NT_UNSTRUCTURED);
            session.save();
            return ticketNode.getPath();
        } catch (RepositoryException e) {
            LOG.error("Error creating ticket", e);
            throw new RuntimeException("Failed to create ticket: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateTicket(ResourceResolver resolver, String ticketPath, String title,
                             String description, TicketPriority priority, String assignee) {
        Resource resource = resolver.getResource(ticketPath);
        if (resource == null) {
            throw new IllegalArgumentException("Ticket not found at path: " + ticketPath);
        }
        ModifiableValueMap props = resource.adaptTo(ModifiableValueMap.class);
        if (props == null) {
            throw new IllegalStateException("Cannot adapt resource to ModifiableValueMap");
        }
        props.put("title", title);
        props.put("description", description);
        if (priority != null) {
            props.put("priority", priority.name());
        }
        if (StringUtils.isNotBlank(assignee)) {
            props.put("assignee", assignee);
        }
        props.put("updated", Calendar.getInstance());
        try {
            resolver.adaptTo(Session.class).save();
        } catch (RepositoryException e) {
            LOG.error("Error updating ticket at {}", ticketPath, e);
            throw new RuntimeException("Failed to update ticket: " + e.getMessage(), e);
        }
    }

    @Override
    public void changeStatus(ResourceResolver resolver, String ticketPath, TicketStatus newStatus) {
        Resource resource = resolver.getResource(ticketPath);
        if (resource == null) {
            throw new IllegalArgumentException("Ticket not found at path: " + ticketPath);
        }
        ModifiableValueMap props = resource.adaptTo(ModifiableValueMap.class);
        if (props == null) {
            throw new IllegalStateException("Cannot adapt resource to ModifiableValueMap");
        }
        String currentStatusStr = props.get("status", TicketStatus.OPEN.name());
        TicketStatus currentStatus = TicketStatus.fromString(currentStatusStr);
        stateService.assertTransitionAllowed(currentStatus, newStatus);
        props.put("status", newStatus.name());
        props.put("updated", Calendar.getInstance());
        try {
            resolver.adaptTo(Session.class).save();
        } catch (RepositoryException e) {
            LOG.error("Error changing status for ticket at {}", ticketPath, e);
            throw new RuntimeException("Failed to change ticket status: " + e.getMessage(), e);
        }
    }

    @Override
    public String addComment(ResourceResolver resolver, String ticketPath, String author, String body) {
        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new IllegalStateException("Cannot obtain JCR session");
            }
            if (!session.nodeExists(ticketPath)) {
                throw new IllegalArgumentException("Ticket not found at path: " + ticketPath);
            }
            Node ticketNode = session.getNode(ticketPath);
            Node commentsNode = JcrUtils.getOrCreateByPath(ticketPath + "/comments", NT_UNSTRUCTURED, session);
            String commentId = "comment-" + UUID.randomUUID().toString().substring(0, 8);
            Node commentNode = commentsNode.addNode(commentId, NT_UNSTRUCTURED);
            commentNode.setProperty("commentId", commentId);
            commentNode.setProperty("author", StringUtils.defaultString(author));
            commentNode.setProperty("body", body);
            commentNode.setProperty("created", Calendar.getInstance());
            ticketNode.setProperty("updated", Calendar.getInstance());
            session.save();
            return commentNode.getPath();
        } catch (RepositoryException e) {
            LOG.error("Error adding comment to ticket at {}", ticketPath, e);
            throw new RuntimeException("Failed to add comment: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getTicket(ResourceResolver resolver, String ticketPath) {
        Resource resource = resolver.getResource(ticketPath);
        if (resource == null) {
            return null;
        }
        return buildTicketMap(resource);
    }

    @Override
    public List<Map<String, Object>> searchTickets(ResourceResolver resolver, String keyword,
                                                    TicketStatus statusFilter, TicketPriority priorityFilter) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return results;
            }

            Map<String, String> predicates = new HashMap<>();
            predicates.put("path", TICKETS_ROOT);
            predicates.put("type", NT_UNSTRUCTURED);
            predicates.put("property", "ticketId");
            predicates.put("property.operation", "exists");

            int propertyIndex = 1;

            if (statusFilter != null) {
                predicates.put(propertyIndex + "_property", "status");
                predicates.put(propertyIndex + "_property.value", statusFilter.name());
                propertyIndex++;
            }

            if (priorityFilter != null) {
                predicates.put(propertyIndex + "_property", "priority");
                predicates.put(propertyIndex + "_property.value", priorityFilter.name());
                propertyIndex++;
            }

            if (StringUtils.isNotBlank(keyword)) {
                String likeValue = "%" + keyword + "%";
                String groupPrefix = "group." + propertyIndex + "_group";
                predicates.put(groupPrefix + ".p.or", "true");
                predicates.put(groupPrefix + ".1_property", "title");
                predicates.put(groupPrefix + ".1_property.operation", "like");
                predicates.put(groupPrefix + ".1_property.value", likeValue);
                predicates.put(groupPrefix + ".2_property", "description");
                predicates.put(groupPrefix + ".2_property.operation", "like");
                predicates.put(groupPrefix + ".2_property.value", likeValue);
            }

            predicates.put("orderby", "@created");
            predicates.put("orderby.sort", "desc");
            predicates.put("p.limit", "-1");

            Query query = queryBuilder.createQuery(PredicateGroup.create(predicates), session);
            SearchResult searchResult = query.getResult();

            for (Hit hit : searchResult.getHits()) {
                Resource ticketResource = hit.getResource();
                if (ticketResource != null) {
                    results.add(buildTicketMap(ticketResource));
                }
            }
        } catch (Exception e) {
            LOG.error("Error searching tickets", e);
        }
        return results;
    }

    private Map<String, Object> buildTicketMap(Resource resource) {
        Map<String, Object> map = new HashMap<>();
        org.apache.sling.api.resource.ValueMap vm = resource.getValueMap();
        map.put("ticketId",    vm.get("ticketId",    ""));
        map.put("path",        resource.getPath());
        map.put("title",       vm.get("title",       ""));
        map.put("description", vm.get("description", ""));
        map.put("priority",    vm.get("priority",    ""));
        map.put("status",      vm.get("status",      ""));
        map.put("assignee",    vm.get("assignee",    ""));
        map.put("reporter",    vm.get("reporter",    ""));
        Calendar created = vm.get("created", Calendar.class);
        Calendar updated = vm.get("updated", Calendar.class);
        map.put("created", created != null ? created.getTimeInMillis() : null);
        map.put("updated", updated != null ? updated.getTimeInMillis() : null);
        List<Map<String, Object>> comments = new ArrayList<>();
        Resource commentsResource = resource.getChild("comments");
        if (commentsResource != null) {
            for (Resource comment : commentsResource.getChildren()) {
                org.apache.sling.api.resource.ValueMap cvm = comment.getValueMap();
                Map<String, Object> cmap = new HashMap<>();
                cmap.put("commentId", cvm.get("commentId", ""));
                cmap.put("author",    cvm.get("author",    ""));
                cmap.put("body",      cvm.get("body",      ""));
                Calendar cCreated = cvm.get("created", Calendar.class);
                cmap.put("created", cCreated != null ? cCreated.getTimeInMillis() : null);
                comments.add(cmap);
            }
        }
        map.put("comments", comments);
        return map;
    }
}
