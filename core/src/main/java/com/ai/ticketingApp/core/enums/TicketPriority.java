package com.ai.ticketingApp.core.enums;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static TicketPriority fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
