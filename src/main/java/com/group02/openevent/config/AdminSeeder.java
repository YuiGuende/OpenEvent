package com.group02.openevent.config;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.admin.Admin;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IAdminRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {
	@Bean
	public CommandLineRunner seedAdmin(IAccountRepo accountRepo, IAdminRepo adminRepo, PasswordEncoder encoder) {
		return args -> {
			final String adminEmail = "admin@gmail.com";
			final String rawPassword = "123456";

			if (accountRepo.existsByEmail(adminEmail)) {
				return;
			}

			Account account = new Account();
			account.setEmail(adminEmail);
			account.setPasswordHash(encoder.encode(rawPassword));
			account.setRole(Role.ADMIN);
			account = accountRepo.save(account);

			Admin admin = new Admin();
			admin.setAccount(account);
			admin.setName("Administrator");
			admin.setEmail(adminEmail);
			adminRepo.save(admin);
		};
	}
} 