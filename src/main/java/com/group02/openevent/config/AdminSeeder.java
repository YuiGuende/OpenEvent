package com.group02.openevent.config;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.admin.Admin;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IAdminRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {
	@Bean
	public CommandLineRunner seedAdmin(IAccountRepo accountRepo, IAdminRepo adminRepo, IUserRepo userRepo, PasswordEncoder encoder, UserService userService) {
		return args -> {
			final String adminEmail = "admin@gmail.com";
			final String rawPassword = "123456";

			if (accountRepo.existsByEmail(adminEmail)) {
				return;
			}

			Account account = new Account();
			account.setEmail(adminEmail);
			account.setPasswordHash(encoder.encode(rawPassword));
			account = accountRepo.save(account);

			// Get or create User for the account
			com.group02.openevent.model.user.User user = userService.getOrCreateUser(account);
			user.setName("Administrator");
			user = userRepo.save(user);

			Admin admin = new Admin();
			admin.setUser(user);
			admin.setName("Administrator");
			admin.setEmail(adminEmail);
			adminRepo.save(admin);
		};
	}
}