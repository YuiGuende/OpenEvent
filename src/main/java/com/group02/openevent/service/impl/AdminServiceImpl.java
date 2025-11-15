package com.group02.openevent.service.impl;


import com.group02.openevent.dto.admin.*;
import com.group02.openevent.model.auditLog.AuditLog;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.event.Speaker;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.*;
import com.group02.openevent.service.AdminService;
import com.group02.openevent.service.AuditLogService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    
    @Autowired
    private IPaymentRepo paymentRepo;
    
    @Autowired
    private IEventAttendanceRepo attendanceRepo;
    
    @Autowired
    private IDepartmentRepo departmentRepo;
    
    @Autowired
    private ISpeakerRepo speakerRepo;
    
    @Autowired
    private IPlaceRepo placeRepo;
    
    @Autowired
    private RequestService requestService;
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private IUserRepo userRepo;
    
    @Autowired
    private ICustomerRepo customerRepo;
    
    @Autowired
    private IHostRepo hostRepo;
    
    @Autowired
    private IAccountRepo accountRepo;
    
    @Autowired
    private IAuditLogRepo auditLogRepo;
    
    @Autowired
    private IFormResponseRepo formResponseRepo;

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

        if ("day".equalsIgnoreCase(period)) {
            // Last 30 days
            LocalDate now = LocalDate.now();
            for (int i = 29; i >= 0; i--) {
                LocalDate day = now.minusDays(i);
                LocalDateTime dayStart = day.atStartOfDay();
                LocalDateTime dayEnd = day.plusDays(1).atStartOfDay(); // End of day = start of next day
                
                Long userCount = auditLogService.countAuditLogsByActionTypeAndDateRange(
                    "USER_CREATED", dayStart, dayEnd);

                Map<String, Object> data = new HashMap<>();
                data.put("day", day.toString());
                data.put("users", userCount);
                result.add(data);
            }
        } else if ("month".equalsIgnoreCase(period)) {
            // Last 6 months
            YearMonth now = YearMonth.now();
            for (int i = 5; i >= 0; i--) {
                YearMonth month = now.minusMonths(i);
                LocalDateTime monthStart = month.atDay(1).atStartOfDay();
                LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();
                
                Long userCount = auditLogService.countAuditLogsByActionTypeAndDateRange(
                    "USER_CREATED", monthStart, monthEnd);

                Map<String, Object> data = new HashMap<>();
                data.put("month", month.toString());
                data.put("users", userCount);
                result.add(data);
            }
        } else if ("year".equalsIgnoreCase(period)) {
            // Last 12 months
            YearMonth now = YearMonth.now();
            for (int i = 11; i >= 0; i--) {
                YearMonth month = now.minusMonths(i);
                LocalDateTime monthStart = month.atDay(1).atStartOfDay();
                LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();
                
                Long userCount = auditLogService.countAuditLogsByActionTypeAndDateRange(
                    "USER_CREATED", monthStart, monthEnd);

                Map<String, Object> data = new HashMap<>();
                data.put("month", month.toString());
                data.put("users", userCount);
                result.add(data);
            }
        } else {
            // Default: Last 30 days
            LocalDate now = LocalDate.now();
            for (int i = 29; i >= 0; i--) {
                LocalDate day = now.minusDays(i);
                LocalDateTime dayStart = day.atStartOfDay();
                LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
                
                Long userCount = auditLogService.countAuditLogsByActionTypeAndDateRange(
                    "USER_CREATED", dayStart, dayEnd);

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
    
    // Financial Reports Implementation
    @Override
    public Page<AdminOrderDTO> getOrders(OrderStatus status, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        List<Order> allOrders = orderRepo.findAll();
        
        // Apply filters
        List<Order> filteredOrders = allOrders.stream()
            .filter(order -> {
                // Status filter
                if (status != null && order.getStatus() != status) {
                    return false;
                }
                
                // Date range filter
                if (fromDate != null && order.getCreatedAt() != null) {
                    if (order.getCreatedAt().toLocalDate().isBefore(fromDate)) {
                        return false;
                    }
                }
                if (toDate != null && order.getCreatedAt() != null) {
                    if (order.getCreatedAt().toLocalDate().isAfter(toDate)) {
                        return false;
                    }
                }
                
                // Search filter
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.toLowerCase();
                    boolean matches = false;
                    if (order.getOrderId() != null && order.getOrderId().toString().contains(search)) {
                        matches = true;
                    }
                    if (order.getEvent() != null && order.getEvent().getTitle() != null && 
                        order.getEvent().getTitle().toLowerCase().contains(searchLower)) {
                        matches = true;
                    }
                    if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
                        if (order.getCustomer().getUser().getName() != null && 
                            order.getCustomer().getUser().getName().toLowerCase().contains(searchLower)) {
                            matches = true;
                        }
                        if (order.getCustomer().getUser().getAccount() != null && 
                            order.getCustomer().getUser().getAccount().getEmail() != null && 
                            order.getCustomer().getUser().getAccount().getEmail().toLowerCase().contains(searchLower)) {
                            matches = true;
                        }
                    }
                    if (order.getParticipantName() != null && 
                        order.getParticipantName().toLowerCase().contains(searchLower)) {
                        matches = true;
                    }
                    if (order.getParticipantEmail() != null && 
                        order.getParticipantEmail().toLowerCase().contains(searchLower)) {
                        matches = true;
                    }
                    if (!matches) return false;
                }
                
                return true;
            })
            .sorted((o1, o2) -> {
                if (o1.getCreatedAt() != null && o2.getCreatedAt() != null) {
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                }
                return 0;
            })
            .collect(Collectors.toList());
        
        // Pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredOrders.size());
        List<Order> pagedOrders = filteredOrders.subList(start, end);
        
        // Convert to DTOs
        List<AdminOrderDTO> orderDTOs = pagedOrders.stream()
            .map(this::convertToAdminOrderDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(orderDTOs, pageable, filteredOrders.size());
    }
    
    @Override
    public Page<AdminPaymentDTO> getPayments(PaymentStatus status, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        List<Payment> allPayments = paymentRepo.findAll();
        
        // Apply filters
        List<Payment> filteredPayments = allPayments.stream()
            .filter(payment -> {
                // Status filter
                if (status != null && payment.getStatus() != status) {
                    return false;
                }
                
                // Date range filter
                if (fromDate != null && payment.getCreatedAt() != null) {
                    if (payment.getCreatedAt().toLocalDate().isBefore(fromDate)) {
                        return false;
                    }
                }
                if (toDate != null && payment.getCreatedAt() != null) {
                    if (payment.getCreatedAt().toLocalDate().isAfter(toDate)) {
                        return false;
                    }
                }
                
                // Search filter
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.toLowerCase();
                    boolean matches = false;
                    if (payment.getPaymentId() != null && payment.getPaymentId().toString().contains(search)) {
                        matches = true;
                    }
                    if (payment.getOrder() != null && payment.getOrder().getOrderId() != null && 
                        payment.getOrder().getOrderId().toString().contains(search)) {
                        matches = true;
                    }
                    if (payment.getTransactionId() != null && 
                        payment.getTransactionId().toLowerCase().contains(searchLower)) {
                        matches = true;
                    }
                    if (payment.getOrder() != null && payment.getOrder().getCustomer() != null && 
                        payment.getOrder().getCustomer().getUser() != null) {
                        if (payment.getOrder().getCustomer().getUser().getName() != null && 
                            payment.getOrder().getCustomer().getUser().getName().toLowerCase().contains(searchLower)) {
                            matches = true;
                        }
                        if (payment.getOrder().getCustomer().getUser().getAccount() != null && 
                            payment.getOrder().getCustomer().getUser().getAccount().getEmail() != null && 
                            payment.getOrder().getCustomer().getUser().getAccount().getEmail().toLowerCase().contains(searchLower)) {
                            matches = true;
                        }
                    }
                    if (!matches) return false;
                }
                
                return true;
            })
            .sorted((p1, p2) -> {
                if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                }
                return 0;
            })
            .collect(Collectors.toList());
        
        // Pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPayments.size());
        List<Payment> pagedPayments = filteredPayments.subList(start, end);
        
        // Convert to DTOs
        List<AdminPaymentDTO> paymentDTOs = pagedPayments.stream()
            .map(this::convertToAdminPaymentDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(paymentDTOs, pageable, filteredPayments.size());
    }
    
    @Override
    public FinancialSummaryDTO getFinancialSummary(LocalDate fromDate, LocalDate toDate) {
        List<Order> allOrders = orderRepo.findAll();
        List<Payment> allPayments = paymentRepo.findAll();
        
        // Apply date filters if provided
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;
        
        List<Order> filteredOrders = allOrders.stream()
            .filter(order -> {
                if (fromDateTime != null && order.getCreatedAt() != null && 
                    order.getCreatedAt().isBefore(fromDateTime)) {
                    return false;
                }
                if (toDateTime != null && order.getCreatedAt() != null && 
                    order.getCreatedAt().isAfter(toDateTime)) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        List<Payment> filteredPayments = allPayments.stream()
            .filter(payment -> {
                if (fromDateTime != null && payment.getCreatedAt() != null && 
                    payment.getCreatedAt().isBefore(fromDateTime)) {
                    return false;
                }
                if (toDateTime != null && payment.getCreatedAt() != null && 
                    payment.getCreatedAt().isAfter(toDateTime)) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        // Calculate metrics
        Long totalRevenue = filteredOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PAID)
            .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount().longValue() : 0)
            .sum();
        
        Long totalOrders = (long) filteredOrders.size();
        
        BigDecimal avgOrderValue = totalOrders > 0 ? 
            BigDecimal.valueOf(totalRevenue).divide(BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        // Calculate service fees (assuming 5% of revenue)
        Long totalServiceFees = (long) (totalRevenue * 0.05);
        
        // Calculate host payouts (95% of revenue)
        Long totalHostPayouts = (long) (totalRevenue * 0.95);
        
        // Net profit (service fees - costs, simplified)
        Long netProfit = totalServiceFees;
        
        // Pending payments
        Long pendingPaymentsAmount = filteredPayments.stream()
            .filter(p -> p.getStatus() == PaymentStatus.PENDING)
            .mapToLong(p -> p.getAmount() != null ? p.getAmount().longValue() : 0)
            .sum();
        
        // Refunded amount
        Long refundedAmount = filteredOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.REFUNDED)
            .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount().longValue() : 0)
            .sum();
        
        // Revenue breakdown by source (currently only ticket sales)
        Map<String, Long> revenueBySource = new HashMap<>();
        revenueBySource.put("Ticket Sales", totalRevenue);
        revenueBySource.put("Service Fees", totalServiceFees);
        
        // Revenue by event type
        Map<String, Long> revenueByEventType = filteredOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PAID && o.getEvent() != null)
            .collect(Collectors.groupingBy(
                o -> o.getEvent().getEventType() != null ? o.getEvent().getEventType().name() : "OTHER",
                Collectors.summingLong(o -> o.getTotalAmount() != null ? o.getTotalAmount().longValue() : 0)
            ));
        
        // Revenue by payment method (simplified - all PayOS for now)
        Map<String, Long> revenueByPaymentMethod = new HashMap<>();
        revenueByPaymentMethod.put("PayOS", totalRevenue);
        
        // Orders by status
        Map<String, Long> ordersByStatus = filteredOrders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getStatus() != null ? o.getStatus().name() : "UNKNOWN",
                Collectors.counting()
            ));
        
        // Payments by status
        Map<String, Long> paymentsByStatus = filteredPayments.stream()
            .collect(Collectors.groupingBy(
                p -> p.getStatus() != null ? p.getStatus().name() : "UNKNOWN",
                Collectors.counting()
            ));
        
        // Conversion rate (simplified)
        long paidOrders = filteredOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PAID)
            .count();
        Double conversionRate = totalOrders > 0 ? (paidOrders * 100.0 / totalOrders) : 0.0;
        
        // Refund rate
        long refundedOrders = filteredOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.REFUNDED)
            .count();
        Double refundRate = totalOrders > 0 ? (refundedOrders * 100.0 / totalOrders) : 0.0;
        
        // Growth rate (simplified - compare with previous period)
        Double revenueGrowthRate = 0.0; // TODO: Calculate based on previous period
        Double orderGrowthRate = 0.0; // TODO: Calculate based on previous period
        
        return FinancialSummaryDTO.builder()
            .totalRevenue(totalRevenue)
            .totalOrders(totalOrders)
            .averageOrderValue(avgOrderValue)
            .totalServiceFees(totalServiceFees)
            .totalHostPayouts(totalHostPayouts)
            .netProfit(netProfit)
            .pendingPaymentsAmount(pendingPaymentsAmount)
            .refundedAmount(refundedAmount)
            .revenueGrowthRate(revenueGrowthRate)
            .orderGrowthRate(orderGrowthRate)
            .revenueBySource(revenueBySource)
            .revenueByEventType(revenueByEventType)
            .revenueByPaymentMethod(revenueByPaymentMethod)
            .ordersByStatus(ordersByStatus)
            .paymentsByStatus(paymentsByStatus)
            .conversionRate(conversionRate)
            .refundRate(refundRate)
            .build();
    }
    
    @Override
    public List<AdminOrderDTO> getAllOrdersForExport(OrderStatus status, String search, LocalDate fromDate, LocalDate toDate) {
        Pageable pageable = Pageable.unpaged();
        Page<AdminOrderDTO> page = getOrders(status, search, fromDate, toDate, pageable);
        return page.getContent();
    }
    
    @Override
    public List<AdminPaymentDTO> getAllPaymentsForExport(PaymentStatus status, String search, LocalDate fromDate, LocalDate toDate) {
        Pageable pageable = Pageable.unpaged();
        Page<AdminPaymentDTO> page = getPayments(status, search, fromDate, toDate, pageable);
        return page.getContent();
    }
    
    // Helper methods
    private AdminOrderDTO convertToAdminOrderDTO(Order order) {
        String customerName = null;
        String customerEmail = null;
        Long customerId = null;
        if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
            customerId = order.getCustomer().getCustomerId();
            customerName = order.getCustomer().getUser().getName();
            if (order.getCustomer().getUser().getAccount() != null) {
                customerEmail = order.getCustomer().getUser().getAccount().getEmail();
            }
        }
        
        String eventTitle = null;
        Long eventId = null;
        if (order.getEvent() != null) {
            eventId = order.getEvent().getId();
            eventTitle = order.getEvent().getTitle();
        }
        
        String ticketTypeName = null;
        Long ticketTypeId = null;
        if (order.getTicketType() != null) {
            ticketTypeId = order.getTicketType().getTicketTypeId();
            ticketTypeName = order.getTicketType().getName();
        }
        
        // Get payment status
        String paymentStatus = "N/A";
        Optional<Payment> paymentOpt = paymentRepo.findByOrder(order);
        if (paymentOpt.isPresent()) {
            paymentStatus = paymentOpt.get().getStatus().name();
        }
        
        LocalDateTime paidAt = null;
        if (paymentOpt.isPresent() && paymentOpt.get().getPaidAt() != null) {
            paidAt = paymentOpt.get().getPaidAt();
        }
        
        return AdminOrderDTO.builder()
            .orderId(order.getOrderId())
            .customerId(customerId)
            .customerName(customerName)
            .customerEmail(customerEmail)
            .eventId(eventId)
            .eventTitle(eventTitle)
            .ticketTypeId(ticketTypeId)
            .ticketTypeName(ticketTypeName)
            .quantity(order.getQuantity())
            .totalAmount(order.getTotalAmount())
            .status(order.getStatus())
            .paymentStatus(paymentStatus)
            .createdAt(order.getCreatedAt())
            .paidAt(paidAt)
            .participantName(order.getParticipantName())
            .participantEmail(order.getParticipantEmail())
            .participantPhone(order.getParticipantPhone())
            .build();
    }
    
    private AdminPaymentDTO convertToAdminPaymentDTO(Payment payment) {
        Long customerId = null;
        String customerName = null;
        String customerEmail = null;
        if (payment.getOrder() != null && payment.getOrder().getCustomer() != null && 
            payment.getOrder().getCustomer().getUser() != null) {
            customerId = payment.getOrder().getCustomer().getCustomerId();
            customerName = payment.getOrder().getCustomer().getUser().getName();
            if (payment.getOrder().getCustomer().getUser().getAccount() != null) {
                customerEmail = payment.getOrder().getCustomer().getUser().getAccount().getEmail();
            }
        }
        
        String paymentMethod = "PayOS"; // Default, can be enhanced
        
        return AdminPaymentDTO.builder()
            .paymentId(payment.getPaymentId())
            .orderId(payment.getOrder() != null ? payment.getOrder().getOrderId() : null)
            .customerId(customerId)
            .customerName(customerName)
            .customerEmail(customerEmail)
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus())
            .paymentMethod(paymentMethod)
            .transactionId(payment.getTransactionId())
            .paymentLinkId(payment.getPaymentLinkId())
            .createdAt(payment.getCreatedAt())
            .paidAt(payment.getPaidAt())
            .expiredAt(payment.getExpiredAt())
            .cancelledAt(payment.getCancelledAt())
            .description(payment.getDescription())
            .build();
    }
    
    // Event Operations methods implementation
    
    @Override
    public Page<PendingApprovalDTO> getPendingApprovals(Pageable pageable) {
        log.info("=== getPendingApprovals called ===");
        log.info("Pageable: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        List<Request> pendingRequests = requestRepo.findByStatus(RequestStatus.PENDING);
        log.info("Total pending requests found: {}", pendingRequests.size());
        
        List<PendingApprovalDTO> dtos = pendingRequests.stream()
            .filter(req -> {
                boolean isEventApproval = req.getType() == RequestType.EVENT_APPROVAL;
                boolean hasEvent = req.getEvent() != null;
                log.debug("Request {}: type={}, hasEvent={}, isEventApproval={}", 
                    req.getRequestId(), req.getType(), hasEvent, isEventApproval);
                return isEventApproval && hasEvent;
            })
            .map(req -> {
                Event event = req.getEvent();
                long daysPending = ChronoUnit.DAYS.between(req.getCreatedAt(), LocalDateTime.now());
                
                String hostName = event.getHost() != null ? event.getHost().getHostName() : "N/A";
                String departmentName = event.getDepartment() != null ? event.getDepartment().getDepartmentName() : "N/A";
                
                log.debug("Processing approval request {} for event {} (days pending: {})", 
                    req.getRequestId(), event.getId(), daysPending);
                
                return PendingApprovalDTO.builder()
                    .requestId(req.getRequestId())
                    .eventId(event.getId())
                    .eventTitle(event.getTitle())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : "N/A")
                    .imageUrl(event.getImageUrl())
                    .hostName(hostName)
                    .departmentName(departmentName)
                    .createdAt(req.getCreatedAt())
                    .daysPending(daysPending)
                    .message(req.getMessage())
                    .fileURL(req.getFileURL())
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getDaysPending(), a.getDaysPending()))
            .collect(Collectors.toList());
        
        log.info("Filtered to {} event approval requests", dtos.size());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());
        List<PendingApprovalDTO> paged = dtos.subList(start, end);
        
        log.info("Returning page with {} items (total: {})", paged.size(), dtos.size());
        
        return new PageImpl<>(paged, pageable, dtos.size());
    }
    
    @Override
    public Page<EventStatusDTO> getEventsByStatus(EventStatus status, EventType eventType, Long departmentId, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        log.info("=== getEventsByStatus called ===");
        log.info("Filters: status={}, eventType={}, departmentId={}, search={}, fromDate={}, toDate={}", 
            status, eventType, departmentId, search, fromDate, toDate);
        log.info("Pageable: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        List<Event> allEvents = eventRepo.findAll();
        log.info("Total events in database: {}", allEvents.size());
        
        List<Event> filtered = allEvents.stream()
            .filter(event -> {
                if (status != null && event.getStatus() != status) return false;
                if (eventType != null && event.getEventType() != eventType) return false;
                if (departmentId != null && (event.getDepartment() == null || !event.getDepartment().getUserId().equals(departmentId))) return false;
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.toLowerCase();
                    if (!event.getTitle().toLowerCase().contains(searchLower)) return false;
                }
                if (fromDate != null && event.getStartsAt() != null && event.getStartsAt().toLocalDate().isBefore(fromDate)) return false;
                if (toDate != null && event.getStartsAt() != null && event.getStartsAt().toLocalDate().isAfter(toDate)) return false;
                return true;
            })
            .sorted((e1, e2) -> {
                if (e1.getCreatedAt() != null && e2.getCreatedAt() != null) {
                    return e2.getCreatedAt().compareTo(e1.getCreatedAt());
                }
                return 0;
            })
            .collect(Collectors.toList());
        
        log.info("Filtered events count: {}", filtered.size());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<Event> paged = filtered.subList(start, end);
        
        log.info("Paged events: {} to {} (total: {})", start, end, filtered.size());
        
        List<EventStatusDTO> dtos = paged.stream()
            .map(event -> {
                long registeredCount = attendanceRepo.countByEventId(event.getId());
                long ticketsSold = orderRepo.findByEventId(event.getId()).stream()
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .mapToLong(o -> o.getQuantity() != null ? o.getQuantity() : 0)
                    .sum();
                long revenue = orderRepo.findByEventId(event.getId()).stream()
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount().longValue() : 0)
                    .sum();
                
                double attendanceRate = 0.0;
                if (registeredCount > 0) {
                    long checkedIn = attendanceRepo.countCheckedInByEventId(event.getId());
                    attendanceRate = (checkedIn * 100.0) / registeredCount;
                }
                
                String departmentName = event.getDepartment() != null ? event.getDepartment().getDepartmentName() : "N/A";
                String hostName = event.getHost() != null ? event.getHost().getHostName() : "N/A";
                
                log.debug("Event {}: registered={}, ticketsSold={}, revenue={}, attendanceRate={}", 
                    event.getId(), registeredCount, ticketsSold, revenue, attendanceRate);
                
                return EventStatusDTO.builder()
                    .eventId(event.getId())
                    .title(event.getTitle())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : "N/A")
                    .status(event.getStatus() != null ? event.getStatus().name() : "N/A")
                    .departmentName(departmentName)
                    .hostName(hostName)
                    .startsAt(event.getStartsAt())
                    .endsAt(event.getEndsAt())
                    .createdAt(event.getCreatedAt())
                    .capacity(event.getCapacity())
                    .registeredCount(registeredCount)
                    .ticketsSold(ticketsSold)
                    .revenue(revenue)
                    .attendanceRate(attendanceRate)
                    .build();
            })
            .collect(Collectors.toList());
        
        log.info("Returning {} DTOs", dtos.size());
        
        return new PageImpl<>(dtos, pageable, filtered.size());
    }
    
    @Override
    public Page<UpcomingEventDTO> getUpcomingEvents(int days, Pageable pageable) {
        log.info("=== getUpcomingEvents called ===");
        log.info("Days: {}, Pageable: page={}, size={}", days, pageable.getPageNumber(), pageable.getPageSize());
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(days);
        log.info("Time range: {} to {}", now, future);
        
        List<Event> allEvents = eventRepo.findAll();
        log.info("Total events in database: {}", allEvents.size());
        
        List<Event> upcoming = allEvents.stream()
            .filter(event -> {
                boolean hasStartTime = event.getStartsAt() != null;
                boolean isAfterNow = hasStartTime && event.getStartsAt().isAfter(now);
                boolean isBeforeFuture = hasStartTime && event.getStartsAt().isBefore(future);
                boolean isPublicOrOngoing = event.getStatus() == EventStatus.PUBLIC || event.getStatus() == EventStatus.ONGOING;
                
                log.debug("Event {}: hasStartTime={}, isAfterNow={}, isBeforeFuture={}, status={}, isPublicOrOngoing={}", 
                    event.getId(), hasStartTime, isAfterNow, isBeforeFuture, event.getStatus(), isPublicOrOngoing);
                
                return hasStartTime && isAfterNow && isBeforeFuture && isPublicOrOngoing;
            })
            .sorted(Comparator.comparing(Event::getStartsAt))
            .collect(Collectors.toList());
        
        log.info("Upcoming events found: {}", upcoming.size());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), upcoming.size());
        List<Event> paged = upcoming.subList(start, end);
        
        log.info("Paged events: {} to {} (total: {})", start, end, upcoming.size());
        
        List<UpcomingEventDTO> dtos = paged.stream()
            .map(event -> {
                long registeredCount = attendanceRepo.countByEventId(event.getId());
                int capacity = event.getCapacity() != null ? event.getCapacity() : 0;
                double fillRate = capacity > 0 ? (registeredCount * 100.0) / capacity : 0.0;
                
                String venue = event.getPlaces() != null && !event.getPlaces().isEmpty() 
                    ? event.getPlaces().get(0).getPlaceName() 
                    : (event.getVenueAddress() != null ? event.getVenueAddress() : "TBA");
                
                String departmentName = event.getDepartment() != null ? event.getDepartment().getDepartmentName() : "N/A";
                String hostName = event.getHost() != null ? event.getHost().getHostName() : "N/A";
                
                long daysUntil = ChronoUnit.DAYS.between(now, event.getStartsAt());
                
                log.debug("Upcoming event {}: registered={}, capacity={}, fillRate={}", 
                    event.getId(), registeredCount, capacity, fillRate);
                
                return UpcomingEventDTO.builder()
                    .eventId(event.getId())
                    .title(event.getTitle())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : "N/A")
                    .status(event.getStatus() != null ? event.getStatus().name() : "N/A")
                    .startsAt(event.getStartsAt())
                    .endsAt(event.getEndsAt())
                    .venue(venue)
                    .registeredCount(registeredCount)
                    .capacity(capacity)
                    .fillRate(fillRate)
                    .departmentName(departmentName)
                    .hostName(hostName)
                    .daysUntil(daysUntil)
                    .build();
            })
            .collect(Collectors.toList());
        
        log.info("Returning {} upcoming event DTOs", dtos.size());
        
        return new PageImpl<>(dtos, pageable, upcoming.size());
    }
    
    @Override
    public List<DepartmentEventStatsDTO> getDepartmentEventStatistics() {
        log.info("=== getDepartmentEventStatistics called ===");
        
        List<Department> departments = departmentRepo.findAll();
        log.info("Total departments found: {}", departments.size());
        
        List<DepartmentEventStatsDTO> result = departments.stream()
            .map(dept -> {
                log.debug("Processing department: {} (ID: {})", dept.getDepartmentName(), dept.getUserId());
                
                List<Event> deptEvents = eventRepo.findAll().stream()
                    .filter(e ->  e.getHost().getUser().getUserId().equals(dept.getUserId()))
                    .collect(Collectors.toList());
                
                log.debug("Department {} has {} events", dept.getDepartmentName(), deptEvents.size());
                
                long totalEvents = deptEvents.size();
                long activeEvents = deptEvents.stream()
                    .filter(e -> e.getStatus() == EventStatus.PUBLIC || e.getStatus() == EventStatus.ONGOING)
                    .count();
                long pendingApproval = requestRepo.findAll().stream()
                    .filter(r -> r.getType() == RequestType.EVENT_APPROVAL 
                        && r.getStatus() == RequestStatus.PENDING
                        && r.getEvent() != null
                        && r.getEvent().getDepartment() != null
                        && r.getEvent().getDepartment().getUserId().equals(dept.getUserId()))
                    .count();
                long completedEvents = deptEvents.stream()
                    .filter(e -> e.getStatus() == EventStatus.FINISH)
                    .count();
                
                long totalRevenue = deptEvents.stream()
                    .flatMap(e -> orderRepo.findByEventId(e.getId()).stream())
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount().longValue() : 0)
                    .sum();
                
                long totalParticipants = deptEvents.stream()
                    .mapToLong(e -> attendanceRepo.countByEventId(e.getId()))
                    .sum();
                
                log.info("Department {} stats: totalEvents={}, activeEvents={}, pendingApproval={}, completedEvents={}, revenue={}, participants={}", 
                    dept.getDepartmentName(), totalEvents, activeEvents, pendingApproval, completedEvents, totalRevenue, totalParticipants);
                
                return DepartmentEventStatsDTO.builder()
                    .departmentId(dept.getUserId())
                    .departmentName(dept.getDepartmentName())
                    .totalEvents(totalEvents)
                    .activeEvents(activeEvents)
                    .pendingApproval(pendingApproval)
                    .completedEvents(completedEvents)
                    .totalRevenue(totalRevenue)
                    .totalParticipants(totalParticipants)
                    .averageRating(0.0) // Can be enhanced with rating system
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getTotalEvents(), a.getTotalEvents()))
            .collect(Collectors.toList());
        
        log.info("Returning {} department statistics", result.size());
        
        return result;
    }
    
    @Override
    public Page<AttendanceStatsDTO> getAttendanceStatistics(EventType eventType, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        log.info("=== getAttendanceStatistics called ===");
        log.info("Filters: eventType={}, fromDate={}, toDate={}", eventType, fromDate, toDate);
        log.info("Pageable: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        List<Event> allEvents = eventRepo.findAll();
        log.info("Total events in database: {}", allEvents.size());
        
        List<Event> filtered = allEvents.stream()
            .filter(event -> {
                if (eventType != null && event.getEventType() != eventType) return false;
                if (fromDate != null && event.getStartsAt() != null && event.getStartsAt().toLocalDate().isBefore(fromDate)) return false;
                if (toDate != null && event.getStartsAt() != null && event.getStartsAt().toLocalDate().isAfter(toDate)) return false;
                return true;
            })
            .collect(Collectors.toList());
        
        log.info("Filtered events count: {}", filtered.size());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<Event> paged = filtered.subList(start, end);
        
        log.info("Paged events: {} to {} (total: {})", start, end, filtered.size());
        
        List<AttendanceStatsDTO> dtos = paged.stream()
            .map(event -> {
                long totalRegistered = attendanceRepo.countByEventId(event.getId());
                long checkedIn = attendanceRepo.countCheckedInByEventId(event.getId());
                long checkedOut = attendanceRepo.countCheckedOutByEventId(event.getId());
                long noShow = totalRegistered - checkedIn;
                
                double attendanceRate = totalRegistered > 0 ? (checkedIn * 100.0) / totalRegistered : 0.0;
                double checkInRate = totalRegistered > 0 ? (checkedIn * 100.0) / totalRegistered : 0.0;
                double checkOutRate = checkedIn > 0 ? (checkedOut * 100.0) / checkedIn : 0.0;
                
                log.debug("Event {} attendance: registered={}, checkedIn={}, checkedOut={}, noShow={}, rate={}%", 
                    event.getId(), totalRegistered, checkedIn, checkedOut, noShow, attendanceRate);
                
                return AttendanceStatsDTO.builder()
                    .eventId(event.getId())
                    .eventTitle(event.getTitle())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : "N/A")
                    .startsAt(event.getStartsAt())
                    .totalRegistered(totalRegistered)
                    .checkedInCount(checkedIn)
                    .checkedOutCount(checkedOut)
                    .noShowCount(noShow)
                    .attendanceRate(attendanceRate)
                    .checkInRate(checkInRate)
                    .checkOutRate(checkOutRate)
                    .build();
            })
            .collect(Collectors.toList());
        
        log.info("Returning {} attendance stats DTOs", dtos.size());
        
        return new PageImpl<>(dtos, pageable, filtered.size());
    }
    
    @Override
    public List<VenueConflictDTO> getVenueConflicts() {
        log.info("=== getVenueConflicts called ===");
        
        List<VenueConflictDTO> conflicts = new ArrayList<>();
        List<Event> allEvents = eventRepo.findAll();
        log.info("Total events in database: {}", allEvents.size());
        
        int checkedPairs = 0;
        for (int i = 0; i < allEvents.size(); i++) {
            Event event1 = allEvents.get(i);
            if (event1.getStartsAt() == null || event1.getEndsAt() == null) {
                log.debug("Event {} skipped: missing start/end time", event1.getId());
                continue;
            }
            if (event1.getPlaces() == null || event1.getPlaces().isEmpty()) {
                log.debug("Event {} skipped: no places", event1.getId());
                continue;
            }
            
            for (int j = i + 1; j < allEvents.size(); j++) {
                Event event2 = allEvents.get(j);
                if (event2.getStartsAt() == null || event2.getEndsAt() == null) continue;
                if (event2.getPlaces() == null || event2.getPlaces().isEmpty()) continue;
                
                checkedPairs++;
                
                // Check if events share a venue
                boolean shareVenue = event1.getPlaces().stream()
                    .anyMatch(p1 -> event2.getPlaces().stream()
                        .anyMatch(p2 -> p1.getId().equals(p2.getId())));
                
                if (!shareVenue) continue;
                
                // Check time overlap
                boolean timeOverlap = !(event1.getEndsAt().isBefore(event2.getStartsAt()) 
                    || event1.getStartsAt().isAfter(event2.getEndsAt()));
                
                if (timeOverlap) {
                    Place sharedPlace = event1.getPlaces().stream()
                        .filter(p1 -> event2.getPlaces().stream()
                            .anyMatch(p2 -> p1.getId().equals(p2.getId())))
                        .findFirst()
                        .orElse(null);
                    
                    LocalDateTime overlapStart = event1.getStartsAt().isAfter(event2.getStartsAt()) 
                        ? event1.getStartsAt() : event2.getStartsAt();
                    LocalDateTime overlapEnd = event1.getEndsAt().isBefore(event2.getEndsAt()) 
                        ? event1.getEndsAt() : event2.getEndsAt();
                    long overlapMinutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                    
                    String severity = overlapMinutes > 60 ? "HIGH" : (overlapMinutes > 30 ? "MEDIUM" : "LOW");
                    
                    log.info("Conflict detected: Event {} and Event {} at venue {} (overlap: {} minutes, severity: {})", 
                        event1.getId(), event2.getId(), 
                        sharedPlace != null ? sharedPlace.getPlaceName() : "Unknown", 
                        overlapMinutes, severity);
                    
                    conflicts.add(VenueConflictDTO.builder()
                        .event1Id(event1.getId())
                        .event1Title(event1.getTitle())
                        .event1Start(event1.getStartsAt())
                        .event1End(event1.getEndsAt())
                        .event2Id(event2.getId())
                        .event2Title(event2.getTitle())
                        .event2Start(event2.getStartsAt())
                        .event2End(event2.getEndsAt())
                        .venueName(sharedPlace != null ? sharedPlace.getPlaceName() : "Unknown")
                        .conflictSeverity(severity)
                        .overlapMinutes(overlapMinutes)
                        .build());
                }
            }
        }
        
        log.info("Checked {} event pairs, found {} conflicts", checkedPairs, conflicts.size());
        
        return conflicts.stream()
            .sorted((a, b) -> Long.compare(b.getOverlapMinutes(), a.getOverlapMinutes()))
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<PointsTrackingDTO> getPointsTracking(EventType eventType, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        log.info("=== getPointsTracking called ===");
        log.info("Filters: eventType={}, fromDate={}, toDate={}", eventType, fromDate, toDate);
        log.info("Pageable: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        List<Event> allEvents = eventRepo.findAll();
        log.info("Total events in database: {}", allEvents.size());
        
        List<Event> filtered = allEvents.stream()
            .filter(event -> {
                if (event.getPoints() == null || event.getPoints() == 0) {
                    log.debug("Event {} skipped: no points", event.getId());
                    return false;
                }
                if (eventType != null && event.getEventType() != eventType) return false;
                if (fromDate != null && event.getStartsAt() != null && event.getStartsAt().toLocalDate().isBefore(fromDate)) return false;
                if (toDate != null && event.getStartsAt() != null && event.getStartsAt().toLocalDate().isAfter(toDate)) return false;
                return true;
            })
            .collect(Collectors.toList());
        
        log.info("Filtered events with points: {}", filtered.size());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<Event> paged = filtered.subList(start, end);
        
        log.info("Paged events: {} to {} (total: {})", start, end, filtered.size());
        
        List<PointsTrackingDTO> dtos = paged.stream()
            .map(event -> {
                long totalParticipants = attendanceRepo.countByEventId(event.getId());
                long studentsEarnedPoints = attendanceRepo.countCheckedInByEventId(event.getId()); // Assuming checked-in = earned points
                double distributionRate = totalParticipants > 0 ? (studentsEarnedPoints * 100.0) / totalParticipants : 0.0;
                
                log.debug("Event {} points: awarded={}, earned={}, participants={}, distributionRate={}%", 
                    event.getId(), event.getPoints(), studentsEarnedPoints, totalParticipants, distributionRate);
                
                return PointsTrackingDTO.builder()
                    .eventId(event.getId())
                    .eventTitle(event.getTitle())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : "N/A")
                    .pointsAwarded(event.getPoints())
                    .studentsEarnedPoints(studentsEarnedPoints)
                    .totalParticipants(totalParticipants)
                    .pointsDistributionRate(distributionRate)
                    .learningObjectives(event.getLearningObjects())
                    .startsAt(event.getStartsAt())
                    .build();
            })
            .collect(Collectors.toList());
        
        log.info("Returning {} points tracking DTOs", dtos.size());
        
        return new PageImpl<>(dtos, pageable, filtered.size());
    }
    
    @Override
    public Page<SpeakerStatsDTO> getSpeakerStatistics(Pageable pageable) {
        log.info("=== getSpeakerStatistics called ===");
        log.info("Pageable: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        List<Speaker> allSpeakers = speakerRepo.findAll();
        log.info("Total speakers in database: {}", allSpeakers.size());
        
        List<SpeakerStatsDTO> dtos = allSpeakers.stream()
            .map(speaker -> {
                List<Event> speakerEvents = speaker.getEvents() != null ? speaker.getEvents() : new ArrayList<>();
                long eventsCount = speakerEvents.size();
                
                long totalParticipants = speakerEvents.stream()
                    .mapToLong(e -> attendanceRepo.countByEventId(e.getId()))
                    .sum();
                
                long totalRevenue = speakerEvents.stream()
                    .flatMap(e -> orderRepo.findByEventId(e.getId()).stream())
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount().longValue() : 0)
                    .sum();
                
                log.debug("Speaker {}: events={}, participants={}, revenue={}", 
                    speaker.getName(), eventsCount, totalParticipants, totalRevenue);
                
                return SpeakerStatsDTO.builder()
                    .speakerId(speaker.getId().longValue())
                    .speakerName(speaker.getName())
                    .role(speaker.getDefaultRole() != null ? speaker.getDefaultRole().name() : "N/A")
                    .imageUrl(speaker.getImageUrl())
                    .eventsCount(eventsCount)
                    .totalParticipants(totalParticipants)
                    .averageRating(0.0) // Can be enhanced with rating system
                    .totalRevenue(totalRevenue)
                    .build();
            })
            .filter(s -> {
                boolean hasEvents = s.getEventsCount() > 0;
                if (!hasEvents) {
                    log.debug("Speaker {} filtered out: no events", s.getSpeakerName());
                }
                return hasEvents;
            })
            .sorted((a, b) -> Long.compare(b.getEventsCount(), a.getEventsCount()))
            .collect(Collectors.toList());
        
        log.info("Filtered to {} speakers with events", dtos.size());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());
        List<SpeakerStatsDTO> paged = dtos.subList(start, end);
        
        log.info("Returning {} speaker stats (total: {})", paged.size(), dtos.size());
        
        return new PageImpl<>(paged, pageable, dtos.size());
    }
    
    @Override
    public Map<String, Object> getEventPerformanceMetrics(LocalDate fromDate, LocalDate toDate) {
        log.info("=== getEventPerformanceMetrics called ===");
        log.info("Date range: fromDate={}, toDate={}", fromDate, toDate);
        
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;
        
        List<Event> allEvents = eventRepo.findAll();
        log.info("Total events in database: {}", allEvents.size());
        
        List<Event> filteredEvents = allEvents.stream()
            .filter(event -> {
                if (fromDateTime != null && event.getStartsAt() != null && event.getStartsAt().isBefore(fromDateTime)) return false;
                if (toDateTime != null && event.getStartsAt() != null && event.getStartsAt().isAfter(toDateTime)) return false;
                return true;
            })
            .collect(Collectors.toList());
        
        log.info("Filtered events count: {}", filteredEvents.size());
        
        long totalEvents = filteredEvents.size();
        long totalRegistrations = filteredEvents.stream()
            .mapToLong(e -> attendanceRepo.countByEventId(e.getId()))
            .sum();
        long totalCheckIns = filteredEvents.stream()
            .mapToLong(e -> attendanceRepo.countCheckedInByEventId(e.getId()))
            .sum();
        long totalRevenue = filteredEvents.stream()
            .flatMap(e -> orderRepo.findByEventId(e.getId()).stream())
            .filter(o -> o.getStatus() == OrderStatus.PAID)
            .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount().longValue() : 0)
            .sum();
        
        log.info("Metrics calculation: totalEvents={}, totalRegistrations={}, totalCheckIns={}, totalRevenue={}", 
            totalEvents, totalRegistrations, totalCheckIns, totalRevenue);
        
        double registrationConversionRate = totalEvents > 0 ? (totalRegistrations * 100.0) / totalEvents : 0.0;
        double attendanceRate = totalRegistrations > 0 ? (totalCheckIns * 100.0) / totalRegistrations : 0.0;
        double revenuePerEvent = totalEvents > 0 ? (double) totalRevenue / totalEvents : 0.0;
        double costPerParticipant = totalCheckIns > 0 ? (double) totalRevenue / totalCheckIns : 0.0;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalEvents", totalEvents);
        metrics.put("totalRegistrations", totalRegistrations);
        metrics.put("totalCheckIns", totalCheckIns);
        metrics.put("totalRevenue", totalRevenue);
        metrics.put("registrationConversionRate", BigDecimal.valueOf(registrationConversionRate).setScale(2, RoundingMode.HALF_UP).doubleValue());
        metrics.put("attendanceRate", BigDecimal.valueOf(attendanceRate).setScale(2, RoundingMode.HALF_UP).doubleValue());
        metrics.put("revenuePerEvent", BigDecimal.valueOf(revenuePerEvent).setScale(2, RoundingMode.HALF_UP).doubleValue());
        metrics.put("costPerParticipant", BigDecimal.valueOf(costPerParticipant).setScale(2, RoundingMode.HALF_UP).doubleValue());
        
        log.info("Returning metrics: registrationRate={}%, attendanceRate={}%, revenuePerEvent={}, costPerParticipant={}", 
            metrics.get("registrationConversionRate"), metrics.get("attendanceRate"), 
            metrics.get("revenuePerEvent"), metrics.get("costPerParticipant"));
        
        return metrics;
    }
    
    @Override
    @Transactional
    public boolean bulkApproveEvents(List<Long> eventIds) {
        try {
            for (Long eventId : eventIds) {
                List<Request> requests = requestRepo.findByEvent_Id(eventId);
                for (Request request : requests) {
                    if (request.getStatus() == RequestStatus.PENDING && request.getType() == RequestType.EVENT_APPROVAL) {
                        requestService.approveRequest(request.getRequestId(), 
                            new com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO("Bulk approved by admin"));
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean bulkRejectEvents(List<Long> eventIds, String reason) {
        try {
            for (Long eventId : eventIds) {
                List<Request> requests = requestRepo.findByEvent_Id(eventId);
                for (Request request : requests) {
                    if (request.getStatus() == RequestStatus.PENDING && request.getType() == RequestType.EVENT_APPROVAL) {
                        com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO dto = 
                            new com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO(reason);
                        requestService.rejectRequest(request.getRequestId(), dto);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean bulkUpdateEventStatus(List<Long> eventIds, EventStatus status) {
        try {
            for (Long eventId : eventIds) {
                eventService.updateEventStatus(eventId, status);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ========== User Activity Methods ==========
    
    @Override
    @Transactional(readOnly = true)
    public UserStatisticsDTO getUserStatistics() {
        // Total users
        long totalUsers = userRepo.count();
        
        // Total customers
        long totalCustomers = customerRepo.count();
        
        // Total hosts
        long totalHosts = hostRepo.count();
        
        // Active users (last 30 days) - users with audit log activity
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long activeUsers = auditLogRepo.findByDateRange(thirtyDaysAgo, LocalDateTime.now())
            .stream()
            .map(AuditLog::getActorId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        
        // New users this month
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newUsersThisMonth = auditLogService.countAuditLogsByActionTypeAndDateRange(
            "USER_CREATED", monthStart, LocalDateTime.now());
        
        // Retention rate calculation (simplified: users active in last 30 days / total users)
        double retentionRate = totalUsers > 0 ? (activeUsers * 100.0 / totalUsers) : 0.0;
        
        // Daily Active Users (DAU) - last 24 hours
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long dau = auditLogRepo.findByDateRange(yesterday, LocalDateTime.now())
            .stream()
            .map(AuditLog::getActorId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        
        // Weekly Active Users (WAU) - last 7 days
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long wau = auditLogRepo.findByDateRange(weekAgo, LocalDateTime.now())
            .stream()
            .map(AuditLog::getActorId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        
        // Monthly Active Users (MAU) - last 30 days
        long mau = activeUsers;
        
        // Average events per user (total events / total users)
        long totalEvents = eventRepo.count();
        double avgEventsPerUser = totalUsers > 0 ? (double) totalEvents / totalUsers : 0.0;
        
        // Average tickets per user (total orders / total customers)
        long totalOrders = orderRepo.count();
        double avgTicketsPerUser = totalCustomers > 0 ? (double) totalOrders / totalCustomers : 0.0;
        
        // Average spending per user
        BigDecimal totalRevenue = BigDecimal.valueOf(calculateTotalRevenue());
        BigDecimal avgSpending = totalCustomers > 0 
            ? totalRevenue.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        // Total feedback count
        long totalFeedbackCount = formResponseRepo.findAll().stream()
            .filter(fr -> fr.getEventForm() != null && 
                fr.getEventForm().getFormType() == com.group02.openevent.model.form.EventForm.FormType.FEEDBACK)
            .count();
        
        return UserStatisticsDTO.builder()
            .totalUsers(totalUsers)
            .totalCustomers(totalCustomers)
            .totalHosts(totalHosts)
            .activeUsers(activeUsers)
            .newUsersThisMonth(newUsersThisMonth)
            .retentionRate(retentionRate)
            .dailyActiveUsers(dau)
            .weeklyActiveUsers(wau)
            .monthlyActiveUsers(mau)
            .averageEventsPerUser(avgEventsPerUser)
            .averageTicketsPerUser(avgTicketsPerUser)
            .averageSpendingPerUser(avgSpending)
            .totalFeedbackCount(totalFeedbackCount)
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserListDTO> getUsers(String role, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        List<com.group02.openevent.model.user.User> allUsers = userRepo.findAll();
        
        // Filter by role
        List<com.group02.openevent.model.user.User> filteredUsers = allUsers.stream()
            .filter(user -> {
                if (role == null || role.isEmpty()) return true;
                com.group02.openevent.model.enums.Role userRole = user.getRole();
                return userRole != null && userRole.name().equalsIgnoreCase(role);
            })
            .filter(user -> {
                if (search == null || search.isEmpty()) return true;
                String searchLower = search.toLowerCase();
                String name = user.getName() != null ? user.getName().toLowerCase() : "";
                String email = user.getAccount() != null && user.getAccount().getEmail() != null 
                    ? user.getAccount().getEmail().toLowerCase() : "";
                return name.contains(searchLower) || email.contains(searchLower);
            })
            .filter(user -> {
                if (fromDate == null && toDate == null) return true;
                LocalDateTime createdAt = user.getCreatedAt();
                if (createdAt == null) return false;
                LocalDate userDate = createdAt.toLocalDate();
                if (fromDate != null && userDate.isBefore(fromDate)) return false;
                if (toDate != null && userDate.isAfter(toDate)) return false;
                return true;
            })
            .collect(Collectors.toList());
        
        // Convert to DTOs
        List<UserListDTO> dtos = filteredUsers.stream()
            .map(user -> {
                // Determine role
                String userRole = "CUSTOMER";
                if (user.hasAdminRole()) userRole = "ADMIN";
                else if (user.hasDepartmentRole()) userRole = "DEPARTMENT";
                else if (user.hasHostRole()) userRole = "HOST";
                
                // Get customer stats
                Long totalEvents = 0L;
                Long totalOrders = 0L;
                Long totalTickets = 0L;
                Long totalSpent = 0L;
                Long totalPoints = 0L;
                Integer feedbackCount = 0;
                
                if (user.getCustomer() != null) {
                    Customer customer = user.getCustomer();
                    totalPoints = customer.getPoints() != null ? customer.getPoints().longValue() : 0L;
                    
                    // Count orders for this customer
                    List<Order> customerOrders = orderRepo.findAll().stream()
                        .filter(o -> o.getCustomer() != null && o.getCustomer().getCustomerId().equals(customer.getCustomerId()))
                        .collect(Collectors.toList());
                    totalOrders = (long) customerOrders.size();
                    
                    // Calculate total tickets and spending
                    for (Order order : customerOrders) {
                        if (order.getQuantity() != null) {
                            totalTickets += order.getQuantity();
                        }
                        if (order.getTotalAmount() != null) {
                            totalSpent += order.getTotalAmount().longValue();
                        }
                    }
                    
                    // Count feedback
                    feedbackCount = formResponseRepo.findAll().stream()
                        .filter(fr -> fr.getCustomer() != null && 
                            fr.getCustomer().getCustomerId().equals(customer.getCustomerId()) &&
                            fr.getEventForm() != null &&
                            fr.getEventForm().getFormType() == com.group02.openevent.model.form.EventForm.FormType.FEEDBACK)
                        .mapToInt(fr -> 1)
                        .sum();
                }
                
                // Get host stats
                if (user.getHost() != null) {
                    totalEvents = (long) eventRepo.findAll().stream()
                        .filter(e -> e.getHost() != null && e.getHost().getId().equals(user.getHost().getId()))
                        .count();
                }
                
                // Get last activity date from audit logs
                LocalDateTime lastActivity = auditLogRepo.findAll().stream()
                    .filter(log -> log.getActorId() != null && log.getActorId().equals(user.getUserId()))
                    .map(com.group02.openevent.model.auditLog.AuditLog::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt());
                
                return UserListDTO.builder()
                    .userId(user.getUserId())
                    .userName(user.getName())
                    .email(user.getAccount() != null ? user.getAccount().getEmail() : "N/A")
                    .role(userRole)
                    .status("ACTIVE") // Simplified - can be enhanced
                    .registrationDate(user.getCreatedAt())
                    .lastActivityDate(lastActivity)
                    .totalEvents(totalEvents)
                    .totalOrders(totalOrders)
                    .totalTickets(totalTickets)
                    .totalSpent(totalSpent)
                    .totalPoints(totalPoints)
                    .feedbackCount(feedbackCount)
                    .build();
            })
            .sorted((a, b) -> {
                // Sort by last activity date descending
                if (a.getLastActivityDate() == null && b.getLastActivityDate() == null) return 0;
                if (a.getLastActivityDate() == null) return 1;
                if (b.getLastActivityDate() == null) return -1;
                return b.getLastActivityDate().compareTo(a.getLastActivityDate());
            })
            .collect(Collectors.toList());
        
        // Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());
        List<UserListDTO> paged = dtos.subList(start, end);
        
        return new PageImpl<>(paged, pageable, dtos.size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EnhancedAuditLogDTO> getEnhancedAuditLogs(String actionType, String entityType, Long userId, 
            String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        List<com.group02.openevent.model.auditLog.AuditLog> allLogs = auditLogRepo.findAll();
        
        // Filter logs
        List<com.group02.openevent.model.auditLog.AuditLog> filteredLogs = allLogs.stream()
            .filter(log -> {
                if (actionType != null && !actionType.isEmpty()) {
                    return log.getActionType() != null && log.getActionType().equals(actionType);
                }
                return true;
            })
            .filter(log -> {
                if (entityType != null && !entityType.isEmpty()) {
                    return log.getEntityType() != null && log.getEntityType().equals(entityType);
                }
                return true;
            })
            .filter(log -> {
                if (userId != null) {
                    return log.getActorId() != null && log.getActorId().equals(userId);
                }
                return true;
            })
            .filter(log -> {
                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    String desc = log.getDescription() != null ? log.getDescription().toLowerCase() : "";
                    return desc.contains(searchLower);
                }
                return true;
            })
            .filter(log -> {
                if (fromDate == null && toDate == null) return true;
                LocalDateTime createdAt = log.getCreatedAt();
                if (createdAt == null) return false;
                LocalDate logDate = createdAt.toLocalDate();
                if (fromDate != null && logDate.isBefore(fromDate)) return false;
                if (toDate != null && logDate.isAfter(toDate)) return false;
                return true;
            })
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .collect(Collectors.toList());
        
        // Convert to DTOs
        List<EnhancedAuditLogDTO> dtos = filteredLogs.stream()
            .map(auditLog -> {
                // Get user info
                String userName = "System";
                String userEmail = "N/A";
                String userRole = "SYSTEM";
                Long userIdValue = auditLog.getActorId();
                
                if (userIdValue != null) {
                    Optional<com.group02.openevent.model.user.User> userOpt = userRepo.findById(userIdValue);
                    if (userOpt.isPresent()) {
                        com.group02.openevent.model.user.User user = userOpt.get();
                        userName = user.getName() != null ? user.getName() : "Unknown";
                        userEmail = user.getAccount() != null && user.getAccount().getEmail() != null 
                            ? user.getAccount().getEmail() : "N/A";
                        userRole = user.getRole() != null ? user.getRole().name() : "CUSTOMER";
                    }
                }
                
                // Get entity details
                String entityDetails = "";
                if (auditLog.getEntityType() != null && auditLog.getEntityId() != null) {
                    try {
                        switch (auditLog.getEntityType()) {
                            case "EVENT":
                                Optional<Event> eventOpt = eventRepo.findById(auditLog.getEntityId());
                                if (eventOpt.isPresent()) {
                                    entityDetails = eventOpt.get().getTitle();
                                }
                                break;
                            case "ORDER":
                                Optional<Order> orderOpt = orderRepo.findById(auditLog.getEntityId());
                                if (orderOpt.isPresent()) {
                                    Order order = orderOpt.get();
                                    entityDetails = String.format("Order #%d - %s", 
                                        order.getOrderId(), 
                                        order.getEvent() != null ? order.getEvent().getTitle() : "N/A");
                                }
                                break;
                            case "FEEDBACK":
                                entityDetails = String.format("Feedback Form ID: %d", auditLog.getEntityId());
                                break;
                            default:
                                entityDetails = String.format("%s ID: %d", auditLog.getEntityType(), auditLog.getEntityId());
                        }
                    } catch (Exception e) {
                        log.warn("Error fetching entity details for {} ID {}: {}", 
                            auditLog.getEntityType(), auditLog.getEntityId(), e.getMessage());
                    }
                }
                
                return EnhancedAuditLogDTO.builder()
                    .auditId(auditLog.getAuditId())
                    .timestamp(auditLog.getCreatedAt())
                    .userId(userIdValue)
                    .userName(userName)
                    .userEmail(userEmail)
                    .userRole(userRole)
                    .actionType(auditLog.getActionType())
                    .entityType(auditLog.getEntityType())
                    .entityId(auditLog.getEntityId())
                    .description(auditLog.getDescription())
                    .entityDetails(entityDetails)
                    .build();
            })
            .collect(Collectors.toList());
        
        // Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());
        List<EnhancedAuditLogDTO> paged = dtos.subList(start, end);
        
        return new PageImpl<>(paged, pageable, dtos.size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EnhancedAuditLogDTO> getAllAuditLogsForExport(String actionType, String entityType, Long userId, 
            String search, LocalDate fromDate, LocalDate toDate) {
        // Use the same logic as getEnhancedAuditLogs but return all results without pagination
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<EnhancedAuditLogDTO> page = getEnhancedAuditLogs(actionType, entityType, userId, search, fromDate, toDate, pageable);
        return page.getContent();
    }
}
