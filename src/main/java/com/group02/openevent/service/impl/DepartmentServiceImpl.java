package com.group02.openevent.service.impl;


import com.group02.openevent.dto.department.DepartmentStatsDTO;
import com.group02.openevent.dto.department.FeaturedEventDTO;
import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.model.department.ArticleStatus;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.repository.*;
import com.group02.openevent.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final IDepartmentRepo departmentRepo;
    private final IEventRepo eventRepo;
    private final IRequestRepo requestRepo;
    private final IArticleRepo articleRepo;
    private final IOrderRepo  orderRepo;
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
        List<Order> departmentOrders = orderRepo.findByDepartmentId(departmentId);

        long totalEvents = departmentEvents.size();
        long pendingRequests = requestRepo.countByReceiverAccountIdAndStatus(departmentId, RequestStatus.PENDING);
        long ongoingEvents = departmentEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.ONGOING)
                .count();

        // Calculate total participants from confirmed orders
        long totalParticipants = departmentOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .count();
        Double revenueChangePercent = calculateRevenueChangePercent(departmentOrders);
        Double cancellationRateChangePercent = calculateCancellationRateChangePercent(departmentOrders);
        cancellationRateChangePercent = cancellationRateChangePercent == null ? 0 : cancellationRateChangePercent;

        // Calculate total revenue from confirmed orders
        BigDecimal totalRevenue = departmentOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate average order value
        BigDecimal avgOrderValue = totalParticipants > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalParticipants), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        // Calculate cancellation rate
        long totalOrders = departmentOrders.size();
        long cancelledOrders = departmentOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .count();
        double cancellationRate = totalOrders > 0
                ? (cancelledOrders * 100.0 / totalOrders)
                : 0.0;

        // Count unique customers
        long uniqueCustomers = departmentOrders.stream()
                .map(o -> o.getCustomer().getCustomerId())
                .distinct()
                .count();

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
                .totalRevenue(totalRevenue)
                .avgOrderValue(avgOrderValue)
                .cancellationRate(cancellationRate)
                .uniqueCustomers(uniqueCustomers)
                .totalOrders(totalOrders)
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
                .revenueChangePercent(revenueChangePercent)
                .cancellationRateChangePercent(cancellationRateChangePercent)
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
    private Double calculateRevenueChangePercent(List<Order> orders) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime previousMonthEnd = currentMonthStart;

        BigDecimal currentMonthRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(currentMonthStart))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousMonthRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(previousMonthStart) && o.getCreatedAt().isBefore(previousMonthEnd))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (previousMonthRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return currentMonthRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }

        return currentMonthRevenue.subtract(previousMonthRevenue)
                .divide(previousMonthRevenue, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private Double calculateCancellationRateChangePercent(List<Order> orders) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime previousMonthEnd = currentMonthStart;

        // Current month cancellation rate
        long currentMonthTotal = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(currentMonthStart))
                .count();

        long currentMonthCancelled = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(currentMonthStart))
                .count();

        double currentMonthRate = currentMonthTotal > 0 ? (currentMonthCancelled * 100.0 / currentMonthTotal) : 0.0;

        // Previous month cancellation rate
        long previousMonthTotal = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(previousMonthStart) && o.getCreatedAt().isBefore(previousMonthEnd))
                .count();

        long previousMonthCancelled = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(previousMonthStart) && o.getCreatedAt().isBefore(previousMonthEnd))
                .count();

        double previousMonthRate = previousMonthTotal > 0 ? (previousMonthCancelled * 100.0 / previousMonthTotal) : 0.0;

        return currentMonthRate - previousMonthRate;
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
        result.put("values", data);
        return result;
    }

    @Override
    public Map<String, Object> getParticipantsTrend(Long departmentId) {
        List<Order> orders = orderRepo.findByDepartmentId(departmentId);
        LocalDateTime now = LocalDateTime.now();
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            long participantCount = orders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .filter(o -> o.getCreatedAt() != null)
                    .filter(o -> !o.getCreatedAt().isBefore(monthStart) && o.getCreatedAt().isBefore(monthEnd))
                    .count();

            labels.add("T" + monthStart.getMonthValue());
            data.add(participantCount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("values", data);
        return result;
    }

    @Override
    public Map<String, Object> getRevenueTrend(Long departmentId) {
        List<Order> orders = orderRepo.findByDepartmentId(departmentId);
        LocalDateTime now = LocalDateTime.now();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            BigDecimal monthRevenue = orders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .filter(o -> o.getCreatedAt() != null)
                    .filter(o -> !o.getCreatedAt().isBefore(monthStart) && o.getCreatedAt().isBefore(monthEnd))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            labels.add("T" + monthStart.getMonthValue());
            data.add(monthRevenue);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("values", data);
        return result;
    }

    @Override
    public List<FeaturedEventDTO> getFeaturedEvents(Long departmentId, int limit) {
        List<Order> orders = orderRepo.findByDepartmentId(departmentId);

        // Group orders by event and calculate statistics
        Map<Event, List<Order>> ordersByEvent = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .collect(Collectors.groupingBy(Order::getEvent));

        // Calculate revenue and ticket count for each event
        List<FeaturedEventDTO> featuredEvents = ordersByEvent.entrySet().stream()
                .map(entry -> {
                    Event event = entry.getKey();
                    List<Order> eventOrders = entry.getValue();

                    long ticketsSold = eventOrders.size();
                    BigDecimal totalRevenue = eventOrders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return FeaturedEventDTO.builder()
                            .eventId(event.getId())
                            .title(event.getTitle())
                            .imageUrl(event.getImageUrl())
                            .ticketsSold(ticketsSold)
                            .totalRevenue(totalRevenue)
                            .build();
                })
                .sorted((a, b) -> {
                    // Sort by revenue first, then by tickets sold
                    int revenueCompare = b.getTotalRevenue().compareTo(a.getTotalRevenue());
                    if (revenueCompare != 0) return revenueCompare;
                    return b.getTicketsSold().compareTo(a.getTicketsSold());
                })
                .limit(limit)
                .collect(Collectors.toList());

        // Add rank
        for (int i = 0; i < featuredEvents.size(); i++) {
            featuredEvents.get(i).setRank(i + 1);
        }

        return featuredEvents;
    }

    @Override
    public Page<OrderDTO> getOrdersByDepartment(Long departmentId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status != null) {
            orders = orderRepo.findByDepartmentIdAndStatus(departmentId, status, pageable);
        } else {
            orders = orderRepo.findByDepartmentId(departmentId, pageable);
        }

        return orders.map(this::convertToOrderDTO);
    }

    private OrderDTO convertToOrderDTO(Order order) {
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .eventId(order.getEvent().getId())
                .eventTitle(order.getEvent().getTitle())
                .eventImageUrl(order.getEvent().getImageUrl())
                .customerName(order.getCustomer().getName())
                .customerEmail(order.getCustomer().getEmail())
                .participantName(order.getParticipantName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .ticketTypeName(order.getTicketType().getName())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    public Double getAverageApprovalTime(Long departmentId) {
        List<Request> approvedRequests = requestRepo.findByReceiver_AccountIdAndStatusOrderByUpdatedAtDesc(
                departmentId, RequestStatus.APPROVED);

        if (approvedRequests.isEmpty()) {
            return 0.0;
        }

        double totalHours = approvedRequests.stream()
                .mapToDouble(req -> {
                    if (req.getCreatedAt() != null && req.getUpdatedAt() != null) {
                        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(req.getCreatedAt(), req.getUpdatedAt());
                        return minutes / 60.0;
                    }
                    return 0.0;
                })
                .sum();

        return totalHours / approvedRequests.size();
    }

    @Override
    public Map<String, Object> getApprovalTrendData(Long departmentId) {
        List<Request> allRequests = requestRepo.findByReceiver_AccountId(departmentId);
        LocalDateTime now = LocalDateTime.now();

        List<String> labels = new ArrayList<>();
        List<Long> approvedData = new ArrayList<>();
        List<Long> rejectedData = new ArrayList<>();
        List<Long> pendingData = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            LocalDateTime weekStart = now.minusWeeks(i).with(java.time.temporal.ChronoField.DAY_OF_WEEK, 1)
                    .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekEnd = weekStart.plusWeeks(1);

            long approved = allRequests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.APPROVED)
                    .filter(r -> r.getUpdatedAt() != null)
                    .filter(r -> !r.getUpdatedAt().isBefore(weekStart) && r.getUpdatedAt().isBefore(weekEnd))
                    .count();

            long rejected = allRequests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.REJECTED)
                    .filter(r -> r.getUpdatedAt() != null)
                    .filter(r -> !r.getUpdatedAt().isBefore(weekStart) && r.getUpdatedAt().isBefore(weekEnd))
                    .count();

            long pending = allRequests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING)
                    .filter(r -> r.getCreatedAt() != null)
                    .filter(r -> !r.getCreatedAt().isBefore(weekStart) && r.getCreatedAt().isBefore(weekEnd))
                    .count();

            labels.add("Tuần " + (4 - i));
            approvedData.add(approved);
            rejectedData.add(rejected);
            pendingData.add(pending);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("approved", approvedData);
        result.put("rejected", rejectedData);
        result.put("pending", pendingData);
        return result;
    }

    @Override
    public Map<String, Object> getRevenueTrendData(Long departmentId) {
        List<Order> orders = orderRepo.findByDepartmentId(departmentId);
        LocalDateTime now = LocalDateTime.now();

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            BigDecimal monthRevenue = orders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .filter(o -> o.getCreatedAt() != null)
                    .filter(o -> !o.getCreatedAt().isBefore(monthStart) && o.getCreatedAt().isBefore(monthEnd))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            labels.add("Tháng " + monthStart.getMonthValue());
            data.add(monthRevenue.doubleValue() / 1_000_000); // Convert to millions
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getOrderStatusDistribution(Long departmentId) {
        List<Order> orders = orderRepo.findByDepartmentId(departmentId);

        // Count orders by status
        Map<OrderStatus, Long> statusCount = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        List<String> backgroundColor = new ArrayList<>();

        // Define colors for each status
        Map<OrderStatus, String> statusColors = new HashMap<>();
        statusColors.put(OrderStatus.PENDING, "rgba(245, 158, 11, 0.8)");      // Amber
        statusColors.put(OrderStatus.PAID, "rgba(16, 185, 129, 0.8)");         // Blue
        statusColors.put(OrderStatus.CANCELLED, "rgba(239, 68, 68, 0.8)");     // Red
        statusColors.put(OrderStatus.EXPIRED, "rgba(107, 114, 128, 0.8)");     // Gray
        statusColors.put(OrderStatus.REFUNDED, "rgba(168, 85, 247, 0.8)");     // Purple

        // Add data for each status
        for (OrderStatus status : OrderStatus.values()) {
            long count = statusCount.getOrDefault(status, 0L);
            if (count > 0) {
                labels.add(status.toString());
                data.add(count);
                backgroundColor.add(statusColors.get(status));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        result.put("backgroundColor", backgroundColor);
        return result;
    }
}
