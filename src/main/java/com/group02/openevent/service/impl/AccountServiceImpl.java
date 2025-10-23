package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.service.AccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    private IAccountRepo accountRepo ;

    public AccountServiceImpl(IAccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Override
    public Account findAccountById(Long id) {
        return accountRepo.findById(id).orElse(null);
    }

    @Override
    public Account getCurrentAccount(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            throw new RuntimeException("User not logged in");
        }
        return findAccountById(accountId);
    }
}
