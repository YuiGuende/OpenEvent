package com.group02.openevent.ai.service;

import com.group02.openevent.model.email.EmailReminder;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IEmailReminderRepo;
import com.group02.openevent.ai.mapper.AIEventMapper;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentEventServiceReminderTest {

    @Mock private EmailReminderService emailReminderService;
    @Mock private EventService eventService;
    @Mock private AIEventMapper AIEventMapper;
    @Mock private CustomerService customerService;
    @Mock private HostService hostService;
    @Mock private OrganizationService organizationService;
    @Mock private IEventRepo eventRepo;
    @Mock private IEmailReminderRepo emailReminderRepo;

    @InjectMocks private AgentEventService service;

    @BeforeEach
    void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void createOrUpdateEmailReminder_update_existing() {
        EmailReminder existing = new EmailReminder();
        when(emailReminderRepo.findByEventIdAndUserId(1L, 9L))
                .thenReturn(Optional.of(existing));

        service.createOrUpdateEmailReminder(1L, 15, 9L);

        verify(emailReminderRepo).save(any(EmailReminder.class));
        verify(eventService, never()).getEventByEventId(anyLong());
    }

    @Test
    void createOrUpdateEmailReminder_create_new() {
        when(emailReminderRepo.findByEventIdAndUserId(2L, 9L))
                .thenReturn(Optional.empty());
        Event e = new Event(); e.setId(2L);
        when(eventService.getEventByEventId(2L)).thenReturn(Optional.of(e));

        service.createOrUpdateEmailReminder(2L, 10, 9L);

        verify(eventService).getEventByEventId(2L);
        verify(emailReminderRepo).save(any(EmailReminder.class));
    }
}


