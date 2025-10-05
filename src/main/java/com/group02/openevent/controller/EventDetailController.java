package com.group02.openevent.controller;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IEventRepo;
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

	public EventDetailController(IEventRepo eventRepo) {
		this.eventRepo = eventRepo;
	}

	@GetMapping("/event/{eventId}")
	public String eventDetail(@PathVariable Long eventId) {
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
			}

			return ResponseEntity.ok(eventData);
			
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", "Failed to fetch event details: " + e.getMessage());
			return ResponseEntity.internalServerError().body(error);
		}
	}

}


