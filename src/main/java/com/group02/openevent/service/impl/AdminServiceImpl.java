package com.group02.openevent.service.impl;


import com.group02.openevent.dto.admin.AdminStatsDTO;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.IRequestRepo;
import com.group02.openevent.service.AdminService;
import com.group02.openevent.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private IOrderRepo orderRepo;

    @Autowired
    private IEventRepo eventRepo;

    @Autowired
    private IRequestRepo requestRepo;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public AdminStatsDTO getAdminDashboardStats() {
        return getAdminDashboardStatsByPeriod("month", null, null);
    }

    @Override
    public AdminStatsDTO getAdminDashboardStatsByPeriod(String period, LocalDate startDate, LocalDate endDate) {
        // Calculate total system revenue from confirmed orders
        Long totalRevenue = calculateTotalRevenue();

        // Count active events (PUBLIC, ONGOING)
        Long activeEvents = (long) (eventRepo.findByStatus(EventStatus.PUBLIC).size() +
                eventRepo.findByStatus(EventStatus.ONGOING).size());

        Long newUsers = auditLogService.countAuditLogsByActionTypeInDays("USER_CREATED", 30);

        // Count pending approval requests
        Long pendingRequests = (long) requestRepo.findByStatus(RequestStatus.PENDING).size();

        // Calculate revenue change percent
        Double revenueChangePercent = calculateRevenueChangePercent();
        Long approvedEvents = (long) eventRepo.findByStatus(EventStatus.PUBLIC).size();
        Long rejectedEvents = (long) eventRepo.findByStatus(EventStatus.CANCEL).size();
        Long pendingEvents = (long) eventRepo.findByStatus(EventStatus.DRAFT).size();
        return AdminStatsDTO.builder()
                .totalSystemRevenue(totalRevenue)
                .totalActiveEvents(activeEvents)
                .totalNewUsers(newUsers)
                .pendingApprovalRequests(pendingRequests)
                .revenueChangePercent(revenueChangePercent)
                .revenueByMonth(getRevenueByPeriod(period, startDate, endDate))
                .eventsByType(getEventsByType())
                .userRegistrationTrend(getUserRegistrationTrend(period, startDate, endDate))
                .subscriptionPlanStats(getSubscriptionPlanStats())
                .approvedEvents(approvedEvents)
                .rejectedEvents(rejectedEvents)
                .pendingEvents(pendingEvents)
                .build();
    }

    private Long calculateTotalRevenue() {
        return orderRepo.findAll().stream()
                .filter(order ->
                        order.getStatus() == OrderStatus.PAID)
                .mapToLong(order -> order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0)
                .sum();
    }

    private Double calculateRevenueChangePercent() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime previousMonthEnd = currentMonthStart.minusSeconds(1);

        Long currentMonthRevenue = orderRepo.findAll().stream()
                .filter(order ->
                        order.getStatus() == OrderStatus.PAID &&
                                order.getCreatedAt() != null &&
                                order.getCreatedAt().isAfter(currentMonthStart))
                .mapToLong(order -> order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0)
                .sum();

        Long previousMonthRevenue = orderRepo.findAll().stream()
                .filter(order ->
                        order.getStatus() == OrderStatus.PAID &&
                                order.getCreatedAt() != null &&
                                order.getCreatedAt().isAfter(previousMonthStart) &&
                                order.getCreatedAt().isBefore(previousMonthEnd))
                .mapToLong(order -> order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0)
                .sum();

        if (previousMonthRevenue == 0) return 0.0;
        return ((double) (currentMonthRevenue - previousMonthRevenue) / previousMonthRevenue) * 100;
    }

    private List<Map<String, Object>> getRevenueByPeriod(String period, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        if ("day".equalsIgnoreCase(period)) {
            // Last 30 days
            LocalDate now = LocalDate.now();
            for (int i = 29; i >= 0; i--) {
                LocalDate day = now.minusDays(i);
                LocalDateTime dayStart = day.atStartOfDay();
                LocalDateTime dayEnd = day.atTime(23, 59, 59);

                Long revenue = calculateRevenueForPeriod(dayStart, dayEnd);

                Map<String, Object> data = new HashMap<>();
                data.put("date", day.toString());
                data.put("revenue", revenue);
                result.add(data);
            }
        } else if ("year".equalsIgnoreCase(period)) {
            // Last 12 months
            for (int i = 11; i >= 0; i--) {
                YearMonth month = YearMonth.now().minusMonths(i);
                LocalDateTime monthStart = month.atDay(1).atStartOfDay();
                LocalDateTime monthEnd = month.atEndOfMonth().atTime(23, 59, 59);

                Long revenue = calculateRevenueForPeriod(monthStart, monthEnd);

                Map<String, Object> data = new HashMap<>();
                data.put("month", month.toString());
                data.put("revenue", revenue);
                result.add(data);
            }
        } else {
            // Default: Last 6 months
            for (int i = 5; i >= 0; i--) {
                YearMonth month = YearMonth.now().minusMonths(i);
                LocalDateTime monthStart = month.atDay(1).atStartOfDay();
                LocalDateTime monthEnd = month.atEndOfMonth().atTime(23, 59, 59);

                Long revenue = calculateRevenueForPeriod(monthStart, monthEnd);

                Map<String, Object> data = new HashMap<>();
                data.put("month", month.toString());
                data.put("revenue", revenue);
                result.add(data);
            }
        }

        return result;
    }

    private Long calculateRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        return orderRepo.findAll().stream()
                .filter(order ->
                        order.getStatus() == OrderStatus.PAID &&
                                order.getCreatedAt() != null &&
                                order.getCreatedAt().isAfter(start) &&
                                order.getCreatedAt().isBefore(end))
                .mapToLong(order -> order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0)
                .sum();
    }

    private List<Map<String, Object>> getEventsByType() {
        List<Map<String, Object>> result = new ArrayList<>();

        // Get all events and group by type
        Map<EventType, Long> typeCount = eventRepo.findAll().stream()
                .collect(Collectors.groupingBy(event -> event.getEventType(), Collectors.counting()));

        List<String> labels = Arrays.asList("Music", "Festival", "Workshop", "Competition");
        List<Long> data = Arrays.asList(
                typeCount.getOrDefault(EventType.MUSIC, 0L),
                typeCount.getOrDefault(EventType.FESTIVAL, 0L),
                typeCount.getOrDefault(EventType.WORKSHOP, 0L),
                typeCount.getOrDefault(EventType.COMPETITION, 0L)
        );

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("data", data);
        result.add(chartData);

        return result;
    }

    private List<Map<String, Object>> getUserRegistrationTrend(String period, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if ("day".equalsIgnoreCase(period)) {
            // Last 30 days
            for (int i = 29; i >= 0; i--) {
                LocalDate day = now.minusDays(i).toLocalDate();
                Long userCount = auditLogService.countAuditLogsByActionTypeInDays("USER_CREATED", 1);

                Map<String, Object> data = new HashMap<>();
                data.put("day", day.toString());
                data.put("users", userCount);
                result.add(data);
            }
        } else if ("year".equalsIgnoreCase(period)) {
            // Last 12 months
            for (int i = 11; i >= 0; i--) {
                YearMonth month = YearMonth.now().minusMonths(i);
                Long userCount = auditLogService.countAuditLogsByActionTypeInDays("USER_CREATED", 30);

                Map<String, Object> data = new HashMap<>();
                data.put("month", month.toString());
                data.put("users", userCount);
                result.add(data);
            }
        } else {
            // Default: Last 30 days
            for (int i = 29; i >= 0; i--) {
                LocalDate day = now.minusDays(i).toLocalDate();
                Long userCount = auditLogService.countAuditLogsByActionTypeInDays("USER_CREATED", 1);

                Map<String, Object> data = new HashMap<>();
                data.put("day", day.toString());
                data.put("users", userCount);
                result.add(data);
            }
        }

        return result;
    }

    private Map<String, Object> getSubscriptionPlanStats() {
        Map<String, Object> stats = new HashMap<>();

        // Line chart data - revenue by subscription plan over 6 months
        List<Map<String, Object>> planRevenue = new ArrayList<>();
        String[] months = {"T5/2025", "T6/2025", "T7/2025", "T8/2025", "T9/2025", "T10/2025"};

        Map<String, Object> basicData = new HashMap<>();
        basicData.put("label", "Basic Plan");
        basicData.put("data", Arrays.asList(120, 145, 168, 195, 220, 245));
        basicData.put("borderColor", "#2c7be5");
        planRevenue.add(basicData);

        Map<String, Object> proData = new HashMap<>();
        proData.put("label", "Pro Plan");
        proData.put("data", Arrays.asList(280, 320, 365, 410, 460, 510));
        proData.put("borderColor", "#ff6b35");
        planRevenue.add(proData);

        Map<String, Object> premiumData = new HashMap<>();
        premiumData.put("label", "Premium Plan");
        premiumData.put("data", Arrays.asList(180, 215, 250, 290, 330, 380));
        premiumData.put("borderColor", "#00d4ff");
        planRevenue.add(premiumData);

        stats.put("months", months);
        stats.put("planRevenue", planRevenue);

        // Pie chart data - subscription count by plan
        Map<String, Object> planDistribution = new HashMap<>();
        planDistribution.put("labels", Arrays.asList("Basic Plan", "Pro Plan", "Premium Plan"));
        planDistribution.put("data", Arrays.asList(1245, 856, 342));
        planDistribution.put("backgroundColor", Arrays.asList("#2c7be5", "#ff6b35", "#00d4ff"));

        stats.put("planDistribution", planDistribution);

        return stats;
    }
}
