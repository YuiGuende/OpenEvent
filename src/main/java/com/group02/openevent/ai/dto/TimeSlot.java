package com.group02.openevent.ai.dto;

import com.group02.openevent.model.event.Event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

public class TimeSlot {

    private final LocalDateTime start;
    private final LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public boolean overlapsWith(Event item) {
        // check time overlap
        return !(item.getEndsAt().isBefore(this.start) || item.getStartsAt().isAfter(this.end));
    }

    public String format() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE (dd/MM) HH:mm");
        return start.format(formatter) + " → " + end.format(formatter);
    }

    public LocalDateTime getStart() { return start; }

    public LocalDateTime getEnd() { return end; }

    @Override
    public String toString() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return String.format("%s từ %s đến %s",
                start.format(dateFormatter),
                start.format(timeFormatter),
                end.format(timeFormatter));
    }
}
