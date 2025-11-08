package com.group02.openevent.controller;

import com.group02.openevent.ai.service.AgentEventService;
import com.group02.openevent.model.email.EmailReminder;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IEmailReminderRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.scheduler.EmailReminderScheduler;
import com.group02.openevent.service.EmailService;
import com.group02.openevent.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private EmailReminderScheduler scheduler;
    
    @Autowired
    private IEmailReminderRepo emailReminderRepo;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AgentEventService agentEventService;
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private ICustomerRepo customerRepo;

    @GetMapping("/trigger-scheduler")
    public String triggerScheduler() {
        try {
            scheduler.checkAndSendReminders();
            return "✅ Scheduler triggered successfully";
        } catch (Exception e) {
            return "❌ Scheduler error: " + e.getMessage();
        }
    }
    
    @GetMapping("/check-reminders")
    public String checkReminders() {
        try {
            List<EmailReminder> reminders = emailReminderRepo.findByIsSent(false);
            return "Found " + reminders.size() + " pending reminders: " + reminders.toString();
        } catch (Exception e) {
            return "❌ Error checking reminders: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-email")
    public String testEmail() {
        try {
            emailService.sendSimpleEmail(
                "ntlequyen2911@gmail.com",
                "Debug Test Email",
                "This is a test email from debug controller"
            );
            return "✅ Test email sent successfully";
        } catch (Exception e) {
            return "❌ Email error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-save-reminder")
    public String testSaveReminder() {
        try {
            // Test với event ID 8 (Music Festival)
            agentEventService.saveEmailReminder(8L, 5, 3L);
            return "✅ Test reminder saved successfully";
        } catch (Exception e) {
            return "❌ Save reminder error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-find-event")
    public String testFindEvent() {
        try {
            // Test tìm event Music Festival
            Optional<Event> eventOpt = eventService.getFirstEventByTitle("Music Festival");
            if (eventOpt.isPresent()) {
                Event event = eventOpt.get();
                return "✅ Found event: " + event.getTitle() + " (ID: " + event.getId() + ")";
            } else {
                return "❌ Event 'Music Festival' not found";
            }
        } catch (Exception e) {
            return "❌ Find event error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-find-user")
    public String testFindUser() {
        try {
            // Test tìm user với account ID 3
            Optional<Customer> customerOpt = customerRepo.findByUser_Account_AccountId(3L);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                String email = customer.getUser() != null && customer.getUser().getAccount() != null 
                    ? customer.getUser().getAccount().getEmail() : "No email";
                return "✅ Found user: " + email;
            } else {
                return "❌ User with account ID 3 not found";
            }
        } catch (Exception e) {
            return "❌ Find user error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-ai-agent")
    public String testAIAgent() {
        try {
            // Test tạo EventAIAgent trực tiếp
            String testMessage = "Nhắc tôi về Music Festival trước 5 phút";
            int userId = 3;
            
            // Test saveEmailReminder
            agentEventService.saveEmailReminder(8L, 5, 3L);
            return "✅ AI Agent test successful: Reminder saved";
        } catch (Exception e) {
            return "❌ AI Agent test error: " + e.getMessage();
        }
    }
}
