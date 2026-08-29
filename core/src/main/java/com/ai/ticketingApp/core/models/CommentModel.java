package com.ai.ticketingApp.core.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CommentModel {

    private String author;
    private String body;
    private long created;

    public CommentModel() {
        // Required by Jackson.
    }

    public CommentModel(String author, String body, long created) {
        this.author = author;
        this.body = body;
        this.created = created;
    }

    public static CommentModel fromJson(String json, ObjectMapper mapper) throws JsonProcessingException {
        return mapper.readValue(json, CommentModel.class);
    }

    public String getAuthor() { return author; }
    public String getBody() { return body; }
    public long getCreated() { return created; }
}
