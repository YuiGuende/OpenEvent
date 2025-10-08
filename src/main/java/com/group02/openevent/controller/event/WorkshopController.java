package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.event.WorkshopEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.IWorkshopService;
import com.group02.openevent.service.TicketTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WorkshopController {

    private final IWorkshopService workshopService;
    private final TicketTypeService ticketTypeService; // Giả sử workshop cũng có vé

    public WorkshopController(IWorkshopService workshopService, TicketTypeService ticketTypeService) {
        this.workshopService = workshopService;
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping("/workshop/{id}")
    public String getWorkshopEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            WorkshopEventDetailDTO event = workshopService.getWorkshopEventById(id);
            List<EventImage> eventImages = workshopService.getEventImages(id);
            List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(id);

            model.addAttribute("event", event);
            model.addAttribute("eventImages", eventImages);
            model.addAttribute("tickets", ticketTypes);

            // Nhóm lịch trình theo ngày
            Map<LocalDate, List<ScheduleDTO>> schedulesByDay = event.getSchedules().stream()
                    .collect(Collectors.groupingBy(sc -> sc.getStartTime().toLocalDate()));

            // Chuyển sang List để Thymeleaf dễ dàng lặp qua
            List<Map.Entry<LocalDate, List<ScheduleDTO>>> scheduleEntries = schedulesByDay.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey()) // Sắp xếp theo ngày
                    .collect(Collectors.toList());

            model.addAttribute("scheduleEntries", scheduleEntries);

        } catch (Exception e) {
            System.err.println("Error loading workshop event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải dữ liệu sự kiện workshop: " + e.getMessage());
        }
        return "workshop/workshopHome"; // Trả về file workshopHome.html
    }
}