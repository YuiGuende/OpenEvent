package com.group02.openevent.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

	private EmbeddingService service;

	@BeforeEach
	void setup() {
		service = new EmbeddingService("test-token");
	}

	@Test
	void getEmbeddings_emptyList_returnsEmpty() throws Exception {
		assertTrue(service.getEmbeddings(java.util.Collections.emptyList()).isEmpty());
		assertTrue(service.getEmbeddings(null).isEmpty());
	}

	@Test
	void cosineSimilarity_sameLength() {
		float[] a = {1f, 2f, 3f};
		float[] b = {1f, 2f, 3f};
		double sim = service.cosineSimilarity(a, b);
		assertTrue(sim > 0.99 && sim <= 1.0);
	}

	@Test
	void cosineSimilarity_differentLength_throws() {
		float[] a = {1f, 2f};
		float[] b = {1f, 2f, 3f};
		assertThrows(IllegalArgumentException.class, () -> service.cosineSimilarity(a, b));
	}

	@Test
	void cosineSimilarity_orthogonal() {
		float[] a = {1f, 0f};
		float[] b = {0f, 1f};
		double sim = service.cosineSimilarity(a, b);
		assertEquals(0.0, sim, 0.001);
	}
}

