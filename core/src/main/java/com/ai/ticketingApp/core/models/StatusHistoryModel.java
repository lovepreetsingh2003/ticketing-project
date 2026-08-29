package com.ai.ticketingApp.core.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StatusHistoryModel {

    private String user;
    private String from;
    private String to;
    private long created;

    public StatusHistoryModel() {
        // Required by Jackson.
    }

    public StatusHistoryModel(String user, String from, String to, long created) {
        this.user = user;
        this.from = from;
        this.to = to;
        this.created = created;
    }

    public static StatusHistoryModel fromJson(String json, ObjectMapper mapper) throws JsonProcessingException {
        return mapper.readValue(json, StatusHistoryModel.class);
    }

    public String getUser() { return user; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public long getCreated() { return created; }
}
