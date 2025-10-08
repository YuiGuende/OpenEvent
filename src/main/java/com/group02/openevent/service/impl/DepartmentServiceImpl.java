package com.group02.openevent.service.impl;


import com.group02.openevent.dto.department.DepartmentStatsDTO;
import com.group02.openevent.model.department.ArticleStatus;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.repository.IArticleRepo;
import com.group02.openevent.repository.IDepartmentRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IRequestRepo;
import com.group02.openevent.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final IDepartmentRepo departmentRepo;
    private final IEventRepo eventRepo;
    private final IRequestRepo requestRepo;
    private final IArticleRepo articleRepo;

    @Override
    public Department getDepartmentByAccountId(Long accountId) {
        return departmentRepo.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Department not found for account ID: " + accountId));
    }

    @Override
    public Department saveDepartment(Department department) {
        return departmentRepo.save(department);
    }

    @Override
    public DepartmentStatsDTO getDepartmentStats(Long departmentId) {
        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // Get all events managed by this department
        List<Event> departmentEvents = department.getEvents();

        // Calculate overview statistics
        long totalEvents = departmentEvents.size();
        long pendingRequests = requestRepo.countByReceiverAccountIdAndStatus(departmentId, RequestStatus.PENDING);
        long ongoingEvents = departmentEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.ONGOING)
                .count();

        // Calculate total participants across all events
        long totalParticipants = departmentEvents.stream()
                .mapToLong(Event::getCapacity)
                .sum();

        // Event statistics by status
        long publicEvents = departmentEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.PUBLIC)
                .count();
        long draftEvents = departmentEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.DRAFT)
                .count();
        long finishedEvents = departmentEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.FINISH)
                .count();
        long cancelledEvents = departmentEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.CANCEL)
                .count();

        // Event statistics by type
        Map<EventType, Long> typeCount = departmentEvents.stream()
                .collect(Collectors.groupingBy(Event::getEventType, Collectors.counting()));

        long musicEvents = typeCount.getOrDefault(EventType.MUSIC, 0L);
        long festivalEvents = typeCount.getOrDefault(EventType.FESTIVAL, 0L);
        long workshopEvents = typeCount.getOrDefault(EventType.WORKSHOP, 0L);
        long competitionEvents = typeCount.getOrDefault(EventType.COMPETITION, 0L);

        // Article statistics
        long totalArticles = articleRepo.countByDepartment(department);
        long publishedArticles = articleRepo.countByDepartmentAndStatus(department, ArticleStatus.PUBLISHED);
        long draftArticles = articleRepo.countByDepartmentAndStatus(department, ArticleStatus.DRAFT);

        // Monthly statistics (last 6 months)
        List<DepartmentStatsDTO.MonthlyEventStats> monthlyStats = calculateMonthlyStats(departmentEvents);

        // Event type distribution for pie chart
        Map<String, Long> eventTypeDistribution = new HashMap<>();
        eventTypeDistribution.put("Music", musicEvents);
        eventTypeDistribution.put("Festival", festivalEvents);
        eventTypeDistribution.put("Workshop", workshopEvents);
        eventTypeDistribution.put("Competition", competitionEvents);

        return DepartmentStatsDTO.builder()
                .totalEvents(totalEvents)
                .pendingRequests(pendingRequests)
                .ongoingEvents(ongoingEvents)
                .totalParticipants(totalParticipants)
                .publicEvents(publicEvents)
                .draftEvents(draftEvents)
                .finishedEvents(finishedEvents)
                .cancelledEvents(cancelledEvents)
                .musicEvents(musicEvents)
                .festivalEvents(festivalEvents)
                .workshopEvents(workshopEvents)
                .competitionEvents(competitionEvents)
                .totalArticles(totalArticles)
                .publishedArticles(publishedArticles)
                .draftArticles(draftArticles)
                .monthlyStats(monthlyStats)
                .eventTypeDistribution(eventTypeDistribution)
                .build();
    }

    private List<DepartmentStatsDTO.MonthlyEventStats> calculateMonthlyStats(List<Event> events) {
        LocalDateTime now = LocalDateTime.now();
        List<DepartmentStatsDTO.MonthlyEventStats> stats = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            long eventCount = events.stream()
                    .filter(e -> e.getCreatedAt() != null)
                    .filter(e -> !e.getCreatedAt().isBefore(monthStart) && e.getCreatedAt().isBefore(monthEnd))
                    .count();

            long participantCount = events.stream()
                    .filter(e -> e.getCreatedAt() != null)
                    .filter(e -> !e.getCreatedAt().isBefore(monthStart) && e.getCreatedAt().isBefore(monthEnd))
                    .mapToLong(Event::getCapacity)
                    .sum();

            String monthLabel = "T" + monthStart.getMonthValue();
            stats.add(new DepartmentStatsDTO.MonthlyEventStats(monthLabel, eventCount, participantCount));
        }

        return stats;
    }

    @Override
    public Map<String, Object> getEventsByMonth(Long departmentId) {
        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<Event> events = department.getEvents();
        LocalDateTime now = LocalDateTime.now();
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            long eventCount = events.stream()
                    .filter(e -> e.getCreatedAt() != null)
                    .filter(e -> !e.getCreatedAt().isBefore(monthStart) && e.getCreatedAt().isBefore(monthEnd))
                    .count();

            labels.add("T" + monthStart.getMonthValue());
            data.add(eventCount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getEventsByType(Long departmentId) {
        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<Event> events = department.getEvents();
        Map<EventType, Long> typeCount = events.stream()
                .collect(Collectors.groupingBy(Event::getEventType, Collectors.counting()));

        List<String> labels = Arrays.asList("Music", "Festival", "Workshop", "Competition");
        List<Long> data = Arrays.asList(
                typeCount.getOrDefault(EventType.MUSIC, 0L),
                typeCount.getOrDefault(EventType.FESTIVAL, 0L),
                typeCount.getOrDefault(EventType.WORKSHOP, 0L),
                typeCount.getOrDefault(EventType.COMPETITION, 0L)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getParticipantsTrend(Long departmentId) {
        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<Event> events = department.getEvents();
        LocalDateTime now = LocalDateTime.now();
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            long participantCount = events.stream()
                    .filter(e -> e.getCreatedAt() != null)
                    .filter(e -> !e.getCreatedAt().isBefore(monthStart) && e.getCreatedAt().isBefore(monthEnd))
                    .mapToLong(Event::getCapacity)
                    .sum();

            labels.add("T" + monthStart.getMonthValue());
            data.add(participantCount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }
}
