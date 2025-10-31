package com.group02.openevent.ai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingServiceTest {

	private RateLimitingService sut;

	@BeforeEach
	void setup() { sut = new RateLimitingService(); }

	@ParameterizedTest
	@EnumSource(RateLimitingService.RateLimitType.class)
	void isAllowed_firstRequest_true(RateLimitingService.RateLimitType type) {
		assertThat(sut.isAllowed("u1", type)).isTrue();
	}
}








