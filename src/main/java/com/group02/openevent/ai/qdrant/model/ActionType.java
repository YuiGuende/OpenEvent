package com.group02.openevent.ai.qdrant.model;

public enum ActionType {
    PROMPT_FREE_TIME("prompt_free_time"),
    PROMPT_SUMMARY_TIME("prompt_summary_time"),
    PROMPT_SEND_EMAIL("prompt_send_email"),
    UNKNOWN("unknown"),
    ERROR("error");

    private final String value;

    ActionType(String value) {
        this.value = value;
    }

    public static ActionType fromString(String value) {
        for (ActionType type : ActionType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public String getValue() {
        return value;
    }
}

