package com.group02.openevent.controller;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class EventDetailController {

	private final IEventRepo eventRepo;
	private final EventService eventService;

	public EventDetailController(IEventRepo eventRepo, EventService eventService) {
		this.eventRepo = eventRepo;
		this.eventService = eventService;
	}

	/**
	 * Router chung: Nhận event ID và tự động route tới đúng controller dựa trên event type
	 */
	@GetMapping("/events/{eventId}")
	public String routeEventById(@PathVariable Long eventId) {
		Optional<Event> eventOpt = eventService.getEventById(eventId);
		
		if (eventOpt.isEmpty()) {
			return "error/404";
		}
		
		Event event = eventOpt.get();
		EventType eventType = event.getEventType();
		
		// Forward tới đúng controller dựa trên event type (không redirect để giữ nguyên URL)
		switch (eventType) {
			case MUSIC:
				return "forward:/music/" + eventId;
			case COMPETITION:
				return "forward:/competition/" + eventId;
			case WORKSHOP:
				return "forward:/workshop/" + eventId;
			case FESTIVAL:
				return "forward:/festival/" + eventId;
			default:
				// Fallback về generic event detail page
				return "forward:/event/" + eventId;
		}
	}

	@GetMapping("/event/{eventId}")
	public String eventDetail(@PathVariable Long eventId, org.springframework.ui.Model model) {
		model.addAttribute("eventId", eventId);
		return "event/event-detail";
	}

	@GetMapping("/event/{eventId}/register")
	public String eventRegister(@PathVariable Long eventId) {
		return "event/event-register";
	}

	/**
	 * API endpoint để lấy thông tin chi tiết event
	 */
	@GetMapping("/api/event/{eventId}")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getEventDetails(@PathVariable Long eventId) {
		try {
			Optional<Event> eventOpt = eventRepo.findById(eventId);
			
			if (eventOpt.isEmpty()) {
				return ResponseEntity.notFound().build();
			}

			Event event = eventOpt.get();
			
			Map<String, Object> eventData = new HashMap<>();
			eventData.put("id", event.getId());
			eventData.put("title", event.getTitle());
			eventData.put("description", event.getDescription());
			eventData.put("type", event.getEventType());
			eventData.put("status", event.getStatus());
			eventData.put("startsAt", event.getStartsAt());
			eventData.put("endsAt", event.getEndsAt());
			eventData.put("enrollDeadline", event.getEnrollDeadline());
			eventData.put("points", event.getPoints());
			eventData.put("benefits", event.getBenefits());
			eventData.put("learningObjects", event.getLearningObjects());
			eventData.put("imageUrl", event.getImageUrl());
			
			// Add host discount information
			if (event.getHost() != null && event.getHost().getHostDiscountPercent() != null) {
				eventData.put("hostDiscountPercent", event.getHost().getHostDiscountPercent());
			} else {
				eventData.put("hostDiscountPercent", 0);
			}

			// Add event-specific data based on type
			String eventType = event.getEventType().toString();
			if ("MUSIC".equals(eventType)) {
				com.group02.openevent.model.event.MusicEvent musicEvent = (com.group02.openevent.model.event.MusicEvent) event;
				eventData.put("genre", musicEvent.getGenre());
				eventData.put("performerCount", musicEvent.getPerformerCount());
			} else if ("WORKSHOP".equals(eventType)) {
				com.group02.openevent.model.event.WorkshopEvent workshopEvent = (com.group02.openevent.model.event.WorkshopEvent) event;
				eventData.put("maxParticipants", workshopEvent.getMaxParticipants());
				eventData.put("skillLevel", workshopEvent.getSkillLevel());
				eventData.put("prerequisites", workshopEvent.getPrerequisites());
			} else if ("COMPETITION".equals(eventType)) {
				com.group02.openevent.model.event.CompetitionEvent competitionEvent = (com.group02.openevent.model.event.CompetitionEvent) event;
				eventData.put("prizePool", competitionEvent.getPrizePool());
				eventData.put("competitionType", competitionEvent.getCompetitionType());
				eventData.put("rules", competitionEvent.getRules());
//			} else if ("ConferenceEvent".equals(eventType)) {
//				com.group02.openevent.model.event.ConferenceEvent conferenceEvent = (com.group02.openevent.model.event.ConferenceEvent) event;
//				eventData.put("conferenceType", conferenceEvent.getConferenceType());
//				eventData.put("maxAttendees", conferenceEvent.getMaxAttendees());
//				eventData.put("agenda", conferenceEvent.getAgenda());
			}

			return ResponseEntity.ok(eventData);
			
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", "Failed to fetch event details: " + e.getMessage());
			return ResponseEntity.internalServerError().body(error);
		}
	}

}


