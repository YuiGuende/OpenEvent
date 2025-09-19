package com.group02.openevent.model.event;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class EventSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private String activity;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public EventSchedule() {
    }

    public EventSchedule(Integer scheduleId, Event event, String activity, LocalDateTime startTime, LocalDateTime endTime) {
        this.scheduleId = scheduleId;
        this.event = event;
        this.activity = activity;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public EventSchedule(Event event, String activity, LocalDateTime startTime, LocalDateTime endTime) {
        this.event = event;
        this.activity = activity;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    // Getter & Setter

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
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

    @Override
    public String toString() {
        return "EventSchedule{" +
                "scheduleId=" + scheduleId +
                ", event=" + event +
                ", activity='" + activity + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
