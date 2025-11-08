package com.group02.openevent.service;

import com.group02.openevent.model.account.Account;
import jakarta.servlet.http.HttpSession;

public interface AccountService {
     Account findAccountById(Long id);
     Account getCurrentAccount(HttpSession session);
}
