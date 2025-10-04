package com.group02.openevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpenEventApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenEventApplication.class, args);
	}

}
