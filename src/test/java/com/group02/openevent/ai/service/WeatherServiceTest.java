package com.group02.openevent.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

	private WeatherService service;

	@BeforeEach
	void setup() {
		service = new WeatherService();
	}

	@Test
	void getForecastNote_withDate_noException() {
		// Method có thể trả về null (API fail), "" (không mưa), hoặc string (có mưa)
		// Chỉ test rằng method không throw exception
		assertDoesNotThrow(() -> 
			service.getForecastNote(LocalDateTime.now(), "Da Nang")
		);
	}

	@Test
	void getForecastNote_futureDate_noException() {
		assertDoesNotThrow(() -> 
			service.getForecastNote(LocalDateTime.now().plusDays(1), "Ho Chi Minh")
		);
	}

	@Test
	void getForecastNote_handlesException() {
		// Test với location có thể gây lỗi encoding hoặc API fail
		// Method nên trả về "" hoặc null khi có exception, không throw
		assertDoesNotThrow(() -> 
			service.getForecastNote(LocalDateTime.now(), "Test@#$Location")
		);
	}
}

