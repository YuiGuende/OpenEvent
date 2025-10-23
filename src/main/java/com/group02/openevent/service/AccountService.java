package com.group02.openevent.service;

import com.group02.openevent.model.account.Account;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

public interface AccountService {
    public Account findAccountById(Long id);
    public Account getCurrentAccount(HttpSession session);
}
