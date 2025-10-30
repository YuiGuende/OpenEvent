package com.group02.openevent.ai.util;

import com.group02.openevent.ai.service.EventAIAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class SessionManagerTest {

    @AfterEach
    void tearDown() {
        SessionManager.clearAll();
    }

    @Test
    void getOrCreate_withDefaultAgent_createsAndReturns() throws Exception {
        EventAIAgent agent = mock(EventAIAgent.class);
        SessionManager.setDefaultAgent(agent);

        String sessionId = "S_TEST_1";
        var result = SessionManager.getOrCreate(sessionId);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, SessionManager.getSessionCount());
    }

    @Test
    void remove_thenGet_throwsNotFound() {
        String sessionId = "S_TEST_2";
        SessionManager.remove(sessionId); // no-op
        try {
            SessionManager.get(sessionId);
            Assertions.fail("Expected exception");
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("Session not found"));
        }
    }
}


