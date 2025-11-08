package com.group02.openevent.controller;

import com.group02.openevent.ai.service.AgentEventService;
import com.group02.openevent.dto.home.TopStudentDTO;
import com.group02.openevent.model.email.EmailReminder;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEmailReminderRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.scheduler.EmailReminderScheduler;
import com.group02.openevent.service.EmailService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.TopStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private IUserRepo userRepo;
    
    @Autowired
    private TopStudentService topStudentService;
    
    @Autowired
    private ICustomerRepo customerRepo;

    @GetMapping("/top-students")
    public Map<String, Object> debugTopStudents() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Get top students
            List<TopStudentDTO> topStudents = topStudentService.getTopStudents(3);
            result.put("success", true);
            result.put("count", topStudents != null ? topStudents.size() : 0);
            result.put("students", topStudents);
            
            // Get all customers from DB
            List<Customer> allCustomers = customerRepo.findAll();
            result.put("totalCustomersInDB", allCustomers.size());
            
            // Get customers with name (using native query)
            List<Object[]> customersWithNameRaw = customerRepo.findTopStudentsByPointsNative();
            result.put("customersWithName", customersWithNameRaw != null ? customersWithNameRaw.size() : 0);
            
            // Sample customers
            List<Map<String, Object>> sampleCustomers = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(5, allCustomers.size()); i++) {
                Customer c = allCustomers.get(i);
                Map<String, Object> sample = new HashMap<>();
                sample.put("id", c.getCustomerId());
                sample.put("name", c.getName());
                sample.put("points", c.getPoints());
                sample.put("email", c.getEmail());
                sampleCustomers.add(sample);
            }
            result.put("sampleCustomers", sampleCustomers);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getName());
        }
        return result;
    }
    
    @GetMapping("/top-students/raw")
    public Map<String, Object> debugTopStudentsRaw() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Get all customers
            List<Customer> allCustomers = customerRepo.findAll();
            result.put("totalCustomers", allCustomers.size());
            
            // Get customers with name (raw query result - native SQL)
            List<Object[]> customersWithNameRaw = customerRepo.findTopStudentsByPointsNative();
            result.put("customersWithNameCount", customersWithNameRaw != null ? customersWithNameRaw.size() : 0);
            
            // Detailed info - parse từ native query result
            List<Map<String, Object>> details = new java.util.ArrayList<>();
            if (customersWithNameRaw != null) {
                for (Object[] row : customersWithNameRaw) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", row[0]); // customer_id
                    detail.put("name", row[1]); // name
                    detail.put("points", row[2]); // points
                    detail.put("email", row[3]); // email
                    detail.put("imageUrl", row[4]); // image_url
                    details.add(detail);
                }
            }
            result.put("customersWithName", details);
            
            // Customers without name
            List<Map<String, Object>> withoutName = new java.util.ArrayList<>();
            for (Customer c : allCustomers) {
                if (c.getName() == null || c.getName().trim().isEmpty()) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", c.getCustomerId());
                    detail.put("name", c.getName());
                    detail.put("points", c.getPoints());
                    detail.put("email", c.getEmail());
                    withoutName.add(detail);
                }
            }
            result.put("customersWithoutName", withoutName);
            result.put("customersWithoutNameCount", withoutName.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getName());
            e.printStackTrace();
        }
        return result;
    }

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
            Optional<Customer> customerOpt = userRepo.findByAccount_AccountId(3L);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                String email = customer.getAccount() != null ? customer.getAccount().getEmail() : "No email";
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
