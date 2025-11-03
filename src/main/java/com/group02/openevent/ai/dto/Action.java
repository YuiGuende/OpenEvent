package com.group02.openevent.ai.dto;

import java.util.HashMap;
import java.util.Map;

public class Action {
    private String toolName;
    private Map<String, Object> args;

    // Default constructor for Jackson
    public Action() {
        this.args = new HashMap<>();
    }

    public Action(String toolName, Map<String, Object> args) {
        this.toolName = toolName;
        this.args = args != null ? args : new HashMap<>();
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }
}
