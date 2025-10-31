package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.ai.dto.EventItem;
import com.group02.openevent.ai.mapper.AIEventMapper;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IEmailReminderRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentEventServiceTest {

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
    void saveEventFromAction_happy() {
        Action action = new Action();
        action.setArgs(Map.of("title","T","start_time","2025-01-01 10:00","end_time","2025-01-01 12:00"));

        Customer c = new Customer();
        Host h = new Host();
        Event e = new Event();
        e.setId(1L);
        when(customerService.getOrCreateByUserId(anyLong())).thenReturn(c);
        when(hostService.findByCustomerId(anyLong())).thenReturn(Optional.of(h));
        when(eventRepo.save(any())).thenReturn(e);

        service.saveEventFromAction(action, 9L);

        verify(eventRepo, times(1)).save(any(Event.class));
        verify(emailReminderRepo, times(1)).findByEventIdAndUserId(eq(1L), eq(9L));
    }

    @Test
    void updateEventFromAction_byId_and_byTitle() {
        Event existing = new Event(); existing.setId(2L);
        when(eventService.getEventByEventId(2L)).thenReturn(Optional.of(existing));
        Action byId = new Action(); byId.setArgs(Map.of("event_id", 2L, "title", "N"));
        service.updateEventFromAction(byId);
        verify(eventService).saveEvent(any(Event.class));

        reset(eventService);
        when(eventService.getFirstEventByTitle("Old")).thenReturn(Optional.of(existing));
        Action byTitle = new Action(); byTitle.setArgs(Map.of("original_title","Old","description","D"));
        service.updateEventFromAction(byTitle);
        verify(eventService).saveEvent(any(Event.class));
    }

    @Test
    void deleteEventFromAction_byId_and_byTitle() {
        Action byId = new Action(); byId.setArgs(Map.of("event_id", 3L));
        service.deleteEventFromAction(byId);
        verify(eventService).removeEvent(3L);

        Action byTitle = new Action(); byTitle.setArgs(Map.of("title","X"));
        service.deleteEventFromAction(byTitle);
        verify(eventService).deleteByTitle("X");
    }

    @Test
    void createEventByCustomer_withOrganization() {
        Customer c = new Customer();
        Host h = new Host();
        when(customerService.getOrCreateByUserId(anyLong())).thenReturn(c);
        when(hostService.findByCustomerId(anyLong())).thenReturn(Optional.of(h));
        Organization org = new Organization();
        when(organizationService.findById(5L)).thenReturn(Optional.of(org));
        Event saved = new Event(); saved.setId(7L);
        when(eventRepo.save(any())).thenReturn(saved);

        EventItem item = new EventItem();
        service.createEventByCustomer(9L, item, 5L);

        verify(eventRepo).save(any(Event.class));
        verify(organizationService).findById(5L);
        verify(emailReminderRepo).findByEventIdAndUserId(7L, 9L);
    }
}


