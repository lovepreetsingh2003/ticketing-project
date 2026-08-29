package com.ai.ticketingApp.core.models;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.enums.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TicketModel {

    private static final Logger LOG = LoggerFactory.getLogger(TicketModel.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ValueMapValue
    private String ticketId;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String priority;

    @ValueMapValue
    private String status;

    @ValueMapValue
    private String assignee;

    @ValueMapValue
    private String reporter;

    @ValueMapValue
    private Calendar created;

    @ValueMapValue
    private Calendar updated;

    private String path;
    private List<CommentModel> comments = new ArrayList<>();
    private List<StatusHistoryModel> statusHistory = new ArrayList<>();
    private List<String> allowedTransitions = new ArrayList<>();

    private Resource resource;

    public TicketModel(Resource resource) {
        this.resource = resource;
    }

    @PostConstruct
    protected void init() {
        if (resource != null) {
            this.path = resource.getPath();
            parseComments(resource.getValueMap().get("comments", new String[0]));
            parseStatusHistory(resource.getValueMap().get("statusHistory", new String[0]));
        }
        TicketStatus currentStatus = TicketStatus.fromString(status);
        if (currentStatus != null) {
            switch (currentStatus) {
                case OPEN:
                    allowedTransitions.add("IN_PROGRESS");
                    allowedTransitions.add("CANCELLED");
                    break;
                case IN_PROGRESS:
                    allowedTransitions.add("RESOLVED");
                    allowedTransitions.add("CANCELLED");
                    break;
                case RESOLVED:
                    allowedTransitions.add("CLOSED");
                    break;
                default:
                    break;
            }
        }
    }

    private void parseComments(String[] entries) {
        for (String entry : entries) {
            try {
                comments.add(CommentModel.fromJson(entry, MAPPER));
            } catch (IOException e) {
                LOG.warn("Ignoring malformed comment entry on {}", path, e);
            }
        }
    }

    private void parseStatusHistory(String[] entries) {
        for (String entry : entries) {
            try {
                statusHistory.add(StatusHistoryModel.fromJson(entry, MAPPER));
            } catch (IOException e) {
                LOG.warn("Ignoring malformed status history entry on {}", path, e);
            }
        }
    }

    public String getTicketId() { return ticketId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public String getReporter() { return reporter; }
    public Calendar getCreated() { return created; }
    public Calendar getUpdated() { return updated; }
    public String getPath() { return path; }
    public List<CommentModel> getComments() { return comments; }
    public List<StatusHistoryModel> getStatusHistory() { return statusHistory; }
    public List<String> getAllowedTransitions() { return allowedTransitions; }
    public boolean isTerminal() {
        return TicketStatus.CLOSED.name().equals(status) || TicketStatus.CANCELLED.name().equals(status);
    }
}
