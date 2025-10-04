package com.group02.openevent.dto;

import java.time.LocalDateTime;

public class ScheduleDTO {
    private String activity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ScheduleDTO(String activity, LocalDateTime startTime, LocalDateTime endTime) {
        this.activity = activity;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
