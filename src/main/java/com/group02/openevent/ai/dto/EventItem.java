package com.group02.openevent.ai.dto;

import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import java.time.LocalDateTime;
import java.util.List;

public class EventItem {

    private Long id;                // ID sự kiện
    private String title;              // event_title
    private String description;        // mô tả
    private LocalDateTime startsAt;    // thời gian bắt đầu
    private LocalDateTime endsAt;
    private String place;// thời gian kết thúc
    private LocalDateTime enrollDeadline; // hạn đăng ký
    private LocalDateTime createdAt;   // ngày tạo
    private EventType eventType;
    private EventStatus eventStatus;
    private String priority;

    public EventItem() {}

    public EventItem(Long id, String title, String description, LocalDateTime startsAt, LocalDateTime endsAt, String place, LocalDateTime enrollDeadline, LocalDateTime createdAt, EventType eventType, EventStatus eventStatus, String priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.place = place;
        this.enrollDeadline = enrollDeadline;
        this.createdAt = createdAt;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
        this.priority = priority;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEnrollDeadline() {
        return enrollDeadline;
    }

    public void setEnrollDeadline(LocalDateTime enrollDeadline) {
        this.enrollDeadline = enrollDeadline;
    }

    public EventStatus getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(EventStatus eventStatus) {
        this.eventStatus = eventStatus;
    }
}