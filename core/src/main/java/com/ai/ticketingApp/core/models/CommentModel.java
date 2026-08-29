package com.ai.ticketingApp.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Calendar;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CommentModel {

    @ValueMapValue
    private String commentId;

    @ValueMapValue
    private String author;

    @ValueMapValue
    private String body;

    @ValueMapValue
    private Calendar created;

    public String getCommentId() { return commentId; }
    public String getAuthor() { return author; }
    public String getBody() { return body; }
    public Calendar getCreated() { return created; }
}
