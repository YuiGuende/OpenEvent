package com.group02.openevent.repository;

import com.group02.openevent.model.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IAccountRepo extends JpaRepository<Account, Long> {
	Optional<Account> findByEmail(String email);
	boolean existsByEmail(String email);
	Optional<Account> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);
} 