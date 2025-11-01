package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.CustomerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private final IUserRepo userRepo;
    @Autowired
    private final IAccountRepo accountRepo;
    @Autowired
    private final ICustomerRepo customerRepo;
    @Autowired
    private final IOrderRepo orderRepo;

    
    @Override
    public Optional<Customer> findByUserId(Long userId) {
        return userRepo.findByAccount_AccountId(userId);
    }
    
    @Override
    public Customer save(Customer customer) {
        return userRepo.save(customer);
    }

    @Override
    public Customer getOrCreateByUserId(Long userId) {
        return userRepo.findByAccount_AccountId(userId).orElseGet(() -> {
            Account account = accountRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account với id=" + userId));
            Customer customer = new Customer();
            customer.setAccount(account);
            customer.setPoints(0);
            return userRepo.save(customer);
        });
    }

    @Override
    public Customer getCustomerByAccountId(Long id) {
        return customerRepo.findByAccount_AccountId(id)
                .orElseThrow(() -> new RuntimeException("Department not found for account ID: " + id));
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
}