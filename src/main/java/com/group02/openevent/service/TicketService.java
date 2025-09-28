package com.group02.openevent.service;

import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.ticket.TicketStatus;
import com.group02.openevent.model.user.User;
import com.group02.openevent.dto.ticket.CreateTicketRequest;
import com.group02.openevent.dto.ticket.TicketResponse;

import java.util.List;
import java.util.Optional;

public interface TicketService {
    
    /**
     * Tạo ticket mới
     * @param request Thông tin tạo ticket
     * @param user User tạo ticket
     * @return Ticket đã tạo
     */
    Ticket createTicket(CreateTicketRequest request, User user);
    
    /**
     * Lấy ticket theo ID
     * @param ticketId ID của ticket
     * @return Optional Ticket
     */
    Optional<Ticket> getTicketById(Long ticketId);
    
    /**
     * Lấy ticket theo ticket code
     * @param ticketCode Mã ticket
     * @return Optional Ticket
     */
    Optional<Ticket> getTicketByCode(String ticketCode);
    
    /**
     * Lấy danh sách tickets theo user
     * @param user User cần lấy tickets
     * @return List Ticket
     */
    List<Ticket> getTicketsByUser(User user);
    
    /**
     * Lấy danh sách tickets theo user ID
     * @param userId ID của user
     * @return List Ticket
     */
    List<Ticket> getTicketsByUserId(Long userId);
    
    /**
     * Lấy danh sách tickets theo user ID và status
     * @param userId ID của user
     * @param status Trạng thái ticket
     * @return List Ticket
     */
    List<Ticket> getTicketsByUserIdAndStatus(Long userId, TicketStatus status);
    
    /**
     * Cập nhật trạng thái ticket
     * @param ticket Ticket cần cập nhật
     * @param status Trạng thái mới
     */
    void updateTicketStatus(Ticket ticket, TicketStatus status);
    
    /**
     * Hủy ticket (chỉ khi chưa thanh toán)
     * @param ticket Ticket cần hủy
     * @return true nếu hủy thành công
     */
    boolean cancelTicket(Ticket ticket);
    
    /**
     * Tạo ticket code duy nhất
     * @return Ticket code
     */
    String generateTicketCode();
    
    /**
     * Kiểm tra và cập nhật tickets hết hạn
     */
    void updateExpiredTickets();
    
    /**
     * Lấy thống kê tickets theo user
     * @param userId ID của user
     * @return TicketResponse chứa thống kê
     */
    TicketResponse getTicketStatistics(Long userId);
    
    /**
     * Kiểm tra user đã đăng ký event chưa
     * @param userId ID của user
     * @param eventId ID của event
     * @return true nếu đã đăng ký
     */
    boolean hasUserRegisteredEvent(Long userId, Long eventId);
    
    // Tích hợp với Guest: User tham gia event sau khi thanh toán thành công
    void joinEventAfterPayment(Long userId, Long eventId);
}
