package com.group02.openevent.service.impl;

import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    
    private final IUserRepo userRepo;
    
    public CustomerServiceImpl(IUserRepo userRepo) {
        this.userRepo = userRepo;
    }
    
    @Override
    public Optional<Customer> findByUserId(Long userId) {
        return userRepo.findByAccount_AccountId(userId);
    }
    
    @Override
    public Customer save(Customer customer) {
        return userRepo.save(customer);
    }
}