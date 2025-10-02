package com.group02.openevent.model.dto.home;


import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCardDTO {
    private Long id;
    private String title;
    private String imageUrl;
    private String description;
    private EventType eventType;
    private EventStatus status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime enrollDeadline;
    private Integer capacity;
    private Integer registered;
    private String city;
    private String organizer;
    private Double price;
    private boolean poster;
    
    // Computed fields for display
    public String getDateLabel() {
        if (startsAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return startsAt.format(formatter);
    }
    
    public String getDurationLabel() {
        if (startsAt == null || endsAt == null) return "";
        Duration duration = Duration.between(startsAt, endsAt);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        
        if (hours > 0 && minutes > 0) {
            return hours + "h" + minutes + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return minutes + "m";
        }
    }
    
    public String getPriceLabel() {
        if (price == null || price == 0) {
            return "Free";
        }
        return String.format("%.0fk", price / 1000);
    }
    
    public boolean isLive() {
        if (startsAt == null || endsAt == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startsAt) && now.isBefore(endsAt);
    }
    
    public boolean isSoldOut() {
        if (capacity == null || registered == null) return false;
        return registered >= capacity;
    }
    
    public String getEventTypeTag() {
        return eventType != null ? eventType.name() : "";
    }
    
    public int getRegistrationPercentage() {
        if (capacity == null || capacity == 0 || registered == null) return 0;
        return Math.min(100, (registered * 100) / capacity);
    }
}
