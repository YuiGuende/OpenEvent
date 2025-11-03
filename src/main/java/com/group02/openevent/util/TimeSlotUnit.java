package com.group02.openevent.util;

import com.group02.openevent.ai.dto.TimeSlot;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.ai.*;
import com.group02.openevent.ai.qdrant.model.TimeContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TimeSlotUnit {

    // Tìm các khoảng thời gian rảnh (mặc định 8h - 22h, slot 2h)
    public static List<TimeSlot> findFreeTime(List<Event> events) {
        List<TimeSlot> allPossibleSlots = generateWeekSlots();

        for (Event event : events) {
            allPossibleSlots.removeIf(slot -> slot.overlapsWith(event));
        }

        return allPossibleSlots;
    }

    private static List<TimeSlot> generateWeekSlots() {
        List<TimeSlot> slots = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            for (int hour = 8; hour <= 22; hour += 2) {
                LocalDateTime start = date.atTime(hour, 0);
                LocalDateTime end = start.plusHours(2);
                slots.add(new TimeSlot(start, end));
            }
        }
        return slots;
    }

    public static Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Xác định bối cảnh thời gian từ input người dùng
    public static TimeContext extractTimeContext(String userInput) {
        String lower = userInput.toLowerCase();
        if (lower.contains("hôm nay")) return TimeContext.TODAY;
        if (lower.contains("ngày mai")) return TimeContext.TOMORROW;
        if (lower.contains("tuần này")) return TimeContext.THIS_WEEK;
        if (lower.contains("tuần sau") || lower.contains("tuần tới")) return TimeContext.NEXT_WEEK;
        if (lower.contains("tháng này")) return TimeContext.THIS_MONTH;

        return TimeContext.UNKNOWN;
    }

    public static List<Event> filterEventsToday(List<Event> events) {
        LocalDate today = LocalDate.now();
        return events.stream()
                .filter(e -> e.getStartsAt().toLocalDate().isEqual(today))
                .collect(Collectors.toList());
    }

    public static List<Event> filterEventsTomorrow(List<Event> events) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return events.stream()
                .filter(e -> e.getStartsAt().toLocalDate().isEqual(tomorrow))
                .collect(Collectors.toList());
    }

    public static List<Event> filterEventsThisWeek(List<Event> events) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        return events.stream()
                .filter(e -> {
                    LocalDate date = e.getStartsAt().toLocalDate();
                    return (date.isEqual(startOfWeek) || date.isAfter(startOfWeek))
                            && (date.isEqual(endOfWeek) || date.isBefore(endOfWeek));
                })
                .collect(Collectors.toList());
    }

    public static List<Event> filterEventsNextWeek(List<Event> events) {
        LocalDate today = LocalDate.now();
        LocalDate startOfNextWeek = today.with(DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate endOfNextWeek = startOfNextWeek.plusDays(6);

        return events.stream()
                .filter(e -> {
                    LocalDate date = e.getStartsAt().toLocalDate();
                    return !date.isBefore(startOfNextWeek) && !date.isAfter(endOfNextWeek);
                })
                .collect(Collectors.toList());
    }
}
