package com.group02.openevent.config;

import com.group02.openevent.ai.service.EventAIAgent;
import com.group02.openevent.ai.util.SessionManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;

@Configuration
public class AIConfig {

    @Autowired
    private EventAIAgent eventAIAgent;

    @PostConstruct
    public void initializeSessionManager() {
        SessionManager.setDefaultAgent(eventAIAgent);
    }
}
