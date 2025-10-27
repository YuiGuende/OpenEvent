package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    
    private final IUserRepo userRepo;
    private final IAccountRepo accountRepo;
    
    public CustomerServiceImpl(IUserRepo userRepo, IAccountRepo accountRepo) {
        this.userRepo = userRepo;
        this.accountRepo = accountRepo;
    }
    
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
}