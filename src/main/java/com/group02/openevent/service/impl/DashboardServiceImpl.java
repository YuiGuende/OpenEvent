package com.group02.openevent.service.impl;

import com.group02.openevent.dto.response.DashboardStatsResponse;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    
    private final IEventRepo eventRepo;
    private final ITicketTypeRepo ticketTypeRepo;
    private final IOrderRepo orderRepo;
    
    // Helper class for daily stats calculation
    private static class DailyStatsData {
        BigDecimal revenue = BigDecimal.ZERO;
        long ordersCount = 0;
        long ticketsSold = 0;
    }
    
    @Override
    public DashboardStatsResponse getEventDashboardStats(Long eventId) {
        log.info("Calculating dashboard stats for event ID: {}", eventId);
        
        // Verify event exists
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        
        // Get ticket types for this event
        List<TicketType> ticketTypes = ticketTypeRepo.findByEventId(eventId);
        log.info("Found {} ticket types for event {}", ticketTypes.size(), eventId);
        
        // Debug: Log ticket type details
        for (TicketType ticketType : ticketTypes) {
            log.info("TicketType ID: {}, Name: {}, Price: {}, TotalQty: {}, SoldQty: {}", 
                ticketType.getTicketTypeId(), ticketType.getName(), ticketType.getPrice(),
                ticketType.getTotalQuantity(), ticketType.getSoldQuantity());
        }
        
        // Get orders for this event
        List<Order> orders = orderRepo.findByEventId(eventId);
        log.info("Found {} orders for event {}", orders.size(), eventId);
        
        // Debug: Log order details
        for (Order order : orders) {
            log.info("Order ID: {}, Status: {}, TicketType: {}, TotalAmount: {}", 
                order.getOrderId(), order.getStatus(), 
                order.getTicketType() != null ? order.getTicketType().getName() : "null",
                order.getTotalAmount());
        }
        
        // Calculate basic statistics
        DashboardStatsResponse.DashboardStatsResponseBuilder builder = DashboardStatsResponse.builder();
        
        // Initialize counters
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalTicketsSold = 0;
        long totalAttendees = 0;
        long totalCheckIn = 0;
        long totalRefunded = 0;
        long totalQuantity = 0;
        long unsoldTickets = 0;
        long vouchersUsed = 0; // TODO: Implement voucher tracking
        
        // Calculate check-in data from orders
        Map<Long, Long> checkInCountByTicketType = new HashMap<>();
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.PAID) {
                Long ticketTypeId = order.getTicketType() != null ? order.getTicketType().getTicketTypeId() : null;
                if (ticketTypeId != null) {
                    checkInCountByTicketType.put(ticketTypeId, 
                        checkInCountByTicketType.getOrDefault(ticketTypeId, 0L) + 1);
                }
            }
        }
        
        // Process ticket types
        List<DashboardStatsResponse.TicketTypeStats> ticketTypeStats = ticketTypes.stream()
                .map(ticketType -> {
                    long soldQty = ticketType.getSoldQuantity() != null ? ticketType.getSoldQuantity() : 0;
                    long totalQty = ticketType.getTotalQuantity() != null ? ticketType.getTotalQuantity() : 0;
                    BigDecimal price = ticketType.getPrice() != null ? ticketType.getPrice() : BigDecimal.ZERO;
                    long checkInCount = checkInCountByTicketType.getOrDefault(ticketType.getTicketTypeId(), 0L);
                    
                    return DashboardStatsResponse.TicketTypeStats.builder()
                            .ticketTypeId(ticketType.getTicketTypeId())
                            .name(ticketType.getName())
                            .price(price)
                            .totalQuantity(totalQty)
                            .soldQuantity(soldQty)
                            .unsoldQuantity(totalQty - soldQty)
                            .checkInCount(checkInCount)
                            .checkInRate(0) // Will be calculated later
                            .build();
                })
                .collect(Collectors.toList());
        
        // Calculate totals from ORDERS instead of ticket types
        // Because TicketType.soldQuantity is not updated, we need to calculate from actual orders
        Map<Long, Long> soldQuantityByTicketType = new HashMap<>();
        Map<Long, BigDecimal> revenueByTicketType = new HashMap<>();
        
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.PAID && order.getTicketType() != null) {
                Long ticketTypeId = order.getTicketType().getTicketTypeId();
                
                // Count sold quantity
                soldQuantityByTicketType.put(ticketTypeId, 
                    soldQuantityByTicketType.getOrDefault(ticketTypeId, 0L) + 1);
                
                // Calculate revenue from order total amount
                BigDecimal orderAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                revenueByTicketType.put(ticketTypeId, 
                    revenueByTicketType.getOrDefault(ticketTypeId, BigDecimal.ZERO).add(orderAmount));
                
                log.info("Order {} contributes: TicketType={}, Amount={}", 
                    order.getOrderId(), order.getTicketType().getName(), orderAmount);
            }
        }
        
        // Update ticket type stats with real data from orders
        for (DashboardStatsResponse.TicketTypeStats stat : ticketTypeStats) {
            Long ticketTypeId = stat.getTicketTypeId();
            long realSoldQty = soldQuantityByTicketType.getOrDefault(ticketTypeId, 0L);
            BigDecimal realRevenue = revenueByTicketType.getOrDefault(ticketTypeId, BigDecimal.ZERO);
            
            // Update the stat with real data
            stat.setSoldQuantity(realSoldQty);
            stat.setUnsoldQuantity(stat.getTotalQuantity() - realSoldQty);
            
            // Add to totals
            totalRevenue = totalRevenue.add(realRevenue);
            totalTicketsSold += realSoldQty;
            totalAttendees += realSoldQty;
            totalCheckIn += stat.getCheckInCount();
            totalQuantity += stat.getTotalQuantity();
            unsoldTickets += stat.getUnsoldQuantity();
            
            log.info("Updated TicketType: {} - RealSoldQty: {}, RealRevenue: {}", 
                stat.getName(), realSoldQty, realRevenue);
        }
        
        // Calculate refunded orders
        totalRefunded = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.REFUNDED)
                .count();
        
        // Update check-in rates for each ticket type
        for (DashboardStatsResponse.TicketTypeStats stat : ticketTypeStats) {
            double checkInRate = stat.getSoldQuantity() > 0 ? (stat.getCheckInCount() * 100.0 / stat.getSoldQuantity()) : 0;
            stat.setCheckInRate(checkInRate);
        }
        
        // Calculate percentage rates
        double checkInRate = totalTicketsSold > 0 ? (totalCheckIn * 100.0 / totalTicketsSold) : 0;
        double refundRate = totalTicketsSold > 0 ? (totalRefunded * 100.0 / totalTicketsSold) : 0;
        double unsoldRate = totalQuantity > 0 ? (unsoldTickets * 100.0 / totalQuantity) : 0;
        
        // Calculate revenue by type for pie chart using real data
        List<DashboardStatsResponse.RevenueByType> revenueByType = new ArrayList<>();
        for (DashboardStatsResponse.TicketTypeStats stat : ticketTypeStats) {
            if (stat.getSoldQuantity() > 0) {
                BigDecimal revenue = revenueByTicketType.getOrDefault(stat.getTicketTypeId(), BigDecimal.ZERO);
                double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0 
                        ? revenue.divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                        : 0;
                
                revenueByType.add(DashboardStatsResponse.RevenueByType.builder()
                        .ticketTypeName(stat.getName())
                        .revenue(revenue)
                        .percentage(percentage)
                        .build());
                
                log.info("Revenue by type: {} - Revenue: {}, Percentage: {}%", 
                    stat.getName(), revenue, percentage);
            }
        }
        
        // Calculate daily stats for trend charts
        Map<String, DailyStatsData> dailyStatsMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (Order order : orders) {
            if (order.getCreatedAt() != null && order.getStatus() != OrderStatus.REFUNDED) {
                String dateKey = order.getCreatedAt().toLocalDate().format(formatter);
                DailyStatsData dailyData = dailyStatsMap.getOrDefault(dateKey, new DailyStatsData());
                
                BigDecimal orderRevenue = BigDecimal.ZERO;
                if (order.getTicketType() != null && order.getTicketType().getPrice() != null) {
                    orderRevenue = order.getTicketType().getPrice();
                }
                
                dailyData.revenue = dailyData.revenue.add(orderRevenue);
                dailyData.ordersCount++;
                dailyData.ticketsSold++;
                
                dailyStatsMap.put(dateKey, dailyData);
            }
        }
        
        // Convert to response format
        List<DashboardStatsResponse.DailyStats> dailyStats = new ArrayList<>();
        for (Map.Entry<String, DailyStatsData> entry : dailyStatsMap.entrySet()) {
            dailyStats.add(DashboardStatsResponse.DailyStats.builder()
                    .date(entry.getKey())
                    .revenue(entry.getValue().revenue)
                    .ordersCount(entry.getValue().ordersCount)
                    .ticketsSold(entry.getValue().ticketsSold)
                    .build());
        }
        
        // Sort by date
        dailyStats.sort((a, b) -> {
            try {
                LocalDate dateA = LocalDate.parse(a.getDate(), formatter);
                LocalDate dateB = LocalDate.parse(b.getDate(), formatter);
                return dateA.compareTo(dateB);
            } catch (Exception e) {
                return 0;
            }
        });
        
        // Build response
        DashboardStatsResponse response = builder
                .totalRevenue(totalRevenue)
                .totalTicketsSold(totalTicketsSold)
                .totalAttendees(totalAttendees)
                .totalCheckIn(totalCheckIn)
                .totalRefunded(totalRefunded)
                .totalQuantity(totalQuantity)
                .unsoldTickets(unsoldTickets)
                .vouchersUsed(vouchersUsed)
                .checkInRate(checkInRate)
                .refundRate(refundRate)
                .unsoldRate(unsoldRate)
                .ticketTypeStats(ticketTypeStats)
                .revenueByType(revenueByType)
                .dailyStats(dailyStats)
                .build();
        
        log.info("Dashboard stats calculated for event {}: Revenue={}, Tickets Sold={}, Check-in Rate={}%", 
                eventId, totalRevenue, totalTicketsSold, checkInRate);
        
        // Debug: Log final calculated values
        log.info("Final KPI Values - Revenue: {}, TicketsSold: {}, Attendees: {}, CheckIn: {}, Refunded: {}, Quantity: {}, Unsold: {}", 
                totalRevenue, totalTicketsSold, totalAttendees, totalCheckIn, totalRefunded, totalQuantity, unsoldTickets);
        log.info("Final Rates - CheckIn: {}%, Refund: {}%, Unsold: {}%", checkInRate, refundRate, unsoldRate);
        
        return response;
    }
}
