package com.group02.openevent.ai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AISecurityServiceTest {

	private AISecurityService sut;

	@BeforeEach
	void setup() { sut = new AISecurityService(); }

	@ParameterizedTest
	@ValueSource(strings = {"<script>x</script>", "UNION SELECT", "javascript:alert(1)"})
	void validateInput_rejects_malicious(String input) {
		var res = sut.validateInput(input, AISecurityService.InputType.MESSAGE);
		assertThat(res.isValid()).isFalse();
	}

	@ParameterizedTest
	@CsvSource({"user@example.com,true","invalid@,false"})
	void validateInput_email(String email, boolean valid) {
		var res = sut.validateInput(email, AISecurityService.InputType.EMAIL);
		assertThat(res.isValid()).isEqualTo(valid);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  "})
	void validateAIResponse_blank_invalid(String resp) {
		var res = sut.validateAIResponse(resp);
		assertThat(res.isValid()).isFalse();
	}
}







