package com.group02.openevent.repository;

import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUserRepo extends JpaRepository<User, Integer> {
} 