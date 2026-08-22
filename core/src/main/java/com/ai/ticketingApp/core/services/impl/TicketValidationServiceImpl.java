package com.ai.ticketingApp.core.services.impl;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.services.TicketValidationService;
import org.osgi.service.component.annotations.Component;
import org.apache.commons.lang3.StringUtils;

@Component(service = TicketValidationService.class)
public class TicketValidationServiceImpl implements TicketValidationService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    @Override
    public String validateCreate(String title, String description, String priority,
                                 String assignee, String reporter) {
        if (StringUtils.isBlank(title)) {
            return "Title is required";
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return "Title must not exceed " + MAX_TITLE_LENGTH + " characters";
        }
        if (StringUtils.isBlank(description)) {
            return "Description is required";
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            return "Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters";
        }
        if (StringUtils.isBlank(priority)) {
            return "Priority is required";
        }
        if (TicketPriority.fromString(priority) == null) {
            return "Invalid priority value: " + priority + ". Valid values: LOW, MEDIUM, HIGH, CRITICAL";
        }
        if (StringUtils.isBlank(assignee)) {
            return "Assignee is required";
        }
        if (StringUtils.isBlank(reporter)) {
            return "Reporter is required";
        }
        return null;
    }

    @Override
    public String validateUpdate(String title, String description, String priority) {
        if (StringUtils.isBlank(title)) {
            return "Title is required";
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return "Title must not exceed " + MAX_TITLE_LENGTH + " characters";
        }
        if (StringUtils.isBlank(description)) {
            return "Description is required";
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            return "Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters";
        }
        if (StringUtils.isNotBlank(priority) && TicketPriority.fromString(priority) == null) {
            return "Invalid priority value: " + priority + ". Valid values: LOW, MEDIUM, HIGH, CRITICAL";
        }
        return null;
    }
}
