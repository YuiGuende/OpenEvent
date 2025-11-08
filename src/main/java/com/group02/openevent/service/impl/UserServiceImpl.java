package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final IUserRepo userRepo;
    private final ICustomerRepo customerRepo;
    
    @Override
    @Transactional
    public User getOrCreateUser(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        
        // Try to find existing User
        Optional<User> existingUser = userRepo.findByAccount(account);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        // Create new User
        return createUserFromAccount(account);
    }

    @Override
    public Optional<User> getUserByAccountId(Long accountId) {
        return userRepo.findByAccount_AccountId(accountId);
    }

    @Override
    public Optional<Customer> getCustomerByAccountId(Long accountId) {
        return customerRepo.findByUser_Account_AccountId(accountId);
    }
    
    @Override
    @Transactional
    public User createUserFromAccount(Account account) {
        User user = new User(account);
        user.setName(account.getEmail()); // Default name is email
        return userRepo.save(user);
    }

    @Override
    public User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            throw new RuntimeException("User not logged in");
        }
        return getUserById(userId);
    }

    @Override
    public User getUserById(Long id) {
        Optional<User> userOptional=userRepo.findById(id);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }
        throw new RuntimeException("User not found "+id);
    }

    @Override
    public List<User> findAllById(List<Long> receiverAccountIds) {
        return userRepo.findAllById(receiverAccountIds);
    }
}

