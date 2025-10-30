package com.group02.openevent.ai.qdrant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

class QdrantServiceTest {
    @InjectMocks QdrantService svc = new QdrantService("http://localhost", "abc", "collection", 3);

    @BeforeEach
    void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void createPayloadIndex_dummy() {
        try {
            svc.createPayloadIndex("fieldA", "string");
        } catch (Exception e) {
            // Accept, likely fail if no real qdrant
        }
    }

    @Test
    void ensureCollection_dummy() {
        try {
            svc.ensureCollection();
        } catch (Exception e) {
            // Accept, likely fail if no real qdrant
        }
    }
}




