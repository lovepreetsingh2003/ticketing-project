package com.ai.ticketingApp.core.models;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.enums.TicketStatus;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TicketModel {

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
    private List<String> allowedTransitions = new ArrayList<>();

    private Resource resource;

    public TicketModel(Resource resource) {
        this.resource = resource;
    }

    @PostConstruct
    protected void init() {
        if (resource != null) {
            this.path = resource.getPath();
            Resource commentsResource = resource.getChild("comments");
            if (commentsResource != null) {
                for (Resource commentRes : commentsResource.getChildren()) {
                    CommentModel comment = commentRes.adaptTo(CommentModel.class);
                    if (comment != null) {
                        comments.add(comment);
                    }
                }
            }
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
    public List<String> getAllowedTransitions() { return allowedTransitions; }
    public boolean isTerminal() {
        return TicketStatus.CLOSED.name().equals(status) || TicketStatus.CANCELLED.name().equals(status);
    }
}
