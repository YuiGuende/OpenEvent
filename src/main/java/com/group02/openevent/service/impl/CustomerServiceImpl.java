package com.group02.openevent.service.impl;

import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.CustomerService;
import com.group02.openevent.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    private final IUserRepo userRepo;
    private final IAccountRepo accountRepo;
    private final ICustomerRepo customerRepo;
    private final IOrderRepo orderRepo;
    private final UserService userService;

    
    @Override
    public Optional<Customer> findByUserId(Long userId) {
        return customerRepo.findByUser_UserId(userId);
    }
    
    @Override
    public Customer save(Customer customer) {
        return customerRepo.save(customer);
    }

    @Override
    public Customer getOrCreateByUserId(Long accountId) {
        return customerRepo.findByUser_Account_AccountId(accountId).orElseGet(() -> {
            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account với id=" + accountId));
            
            // Get or create User
            User user = userService.getOrCreateUser(account);
            
            // Create Customer
            Customer customer = new Customer();
            customer.setUser(user);
            customer.setPoints(0);
            return customerRepo.save(customer);
        });
    }

    @Override
    public Customer getCustomerByAccountId(Long accountId) {
        return customerRepo.findByUser_Account_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Customer not found for account ID: " + accountId));
    }

    @Override
    public Page<OrderDTO> getOrdersByEvent(Long eventId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status != null) {
            orders = orderRepo.findByEventIdAndStatus(eventId, status, pageable);
        } else {
            orders = orderRepo.findByEventId(eventId, pageable);
        }

        return orders.map(this::convertToOrderDTO);
    }

    private OrderDTO convertToOrderDTO(Order order) {
        Customer customer = order.getCustomer();
        String customerName = customer != null && customer.getUser() != null 
            ? customer.getUser().getName() 
            : "Unknown";
        String customerEmail = customer != null && customer.getUser() != null && customer.getUser().getAccount() != null
            ? customer.getUser().getAccount().getEmail()
            : "";
            
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .eventId(order.getEvent().getId())
                .eventTitle(order.getEvent().getTitle())
                .eventImageUrl(order.getEvent().getImageUrl())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .participantName(order.getParticipantName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .ticketTypeName(order.getTicketType().getName())
                .createdAt(order.getCreatedAt())
                .build();
    }
}