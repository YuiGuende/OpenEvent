package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.event.FestivalEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.model.event.FestivalEvent;
import com.group02.openevent.repository.IFestivalEventRepo;
import com.group02.openevent.service.IFestivalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FestivalServiceImpl implements IFestivalService {

    @Autowired
    private IFestivalEventRepo festivalEventRepo;

    @Override
    public List<FestivalEventDetailDTO> getAllFestivalEvents() {
        List<FestivalEvent> festivalEvents = festivalEventRepo.findAll();
        return festivalEvents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FestivalEventDetailDTO getFestivalEventById(Long id) {
        FestivalEvent festivalEvent = festivalEventRepo.findById(id)
                .orElse(null);
        
        if (festivalEvent == null) {
            return null;
        }

        return convertToDTO(festivalEvent);
    }

    @Override
    public List<EventImage> getEventImages(Long eventId) {
        FestivalEvent festivalEvent = festivalEventRepo.findById(eventId)
                .orElse(null);
        
        if (festivalEvent == null) {
            return List.of();
        }
        
        return festivalEvent.getEventImages() != null ?
                festivalEvent.getEventImages().stream().collect(Collectors.toList()) :
                List.of();
    }

    private FestivalEventDetailDTO convertToDTO(FestivalEvent festivalEvent) {
        FestivalEventDetailDTO dto = new FestivalEventDetailDTO();

        // Base Event fields
        dto.setId(festivalEvent.getId());
        dto.setTitle(festivalEvent.getTitle());
        dto.setImageUrl(festivalEvent.getImageUrl());
        dto.setDescription(festivalEvent.getDescription());
        dto.setCapacity(festivalEvent.getCapacity());
        dto.setPublicDate(festivalEvent.getPublicDate());
        dto.setEnrollDeadline(festivalEvent.getEnrollDeadline());
        dto.setStartsAt(festivalEvent.getStartsAt());
        dto.setEndsAt(festivalEvent.getEndsAt());
        dto.setCreatedAt(festivalEvent.getCreatedAt());
        dto.setStatus(festivalEvent.getStatus());
        dto.setBenefits(festivalEvent.getBenefits());
        dto.setLearningObjects(festivalEvent.getLearningObjects());
        dto.setPoints(festivalEvent.getPoints());
        dto.setVenueAddress(festivalEvent.getVenueAddress());
        dto.setGuidelines(festivalEvent.getGuidelines());

        // Festival-specific fields - existing
        dto.setCulture(festivalEvent.getCulture());
        dto.setHighlight(festivalEvent.getHighlight());

        // Festival-specific fields - new
        dto.setFestivalTheme(festivalEvent.getFestivalTheme());
        dto.setNumberOfActivities(festivalEvent.getNumberOfActivities());
        dto.setTargetAudience(festivalEvent.getTargetAudience());
        dto.setRegistrationRequired(festivalEvent.getRegistrationRequired());
        dto.setFestivalType(festivalEvent.getFestivalType());
        dto.setMainOrganizer(festivalEvent.getMainOrganizer());
        dto.setExpectedAttendees(festivalEvent.getExpectedAttendees());

        // Related entities
        dto.setSchedules(festivalEvent.getSchedules().stream()
                .map(schedule -> new com.group02.openevent.model.dto.ScheduleDTO(
                        schedule.getActivity(),
                        schedule.getStartTime(),
                        schedule.getEndTime()
                ))
                .collect(Collectors.toList()));
        dto.setSpeakers(festivalEvent.getSpeakers());
        dto.setPlaces(festivalEvent.getPlaces());
        dto.setEventImages(festivalEvent.getEventImages());
        dto.setOrganizationName(festivalEvent.getOrganization() != null ?
                festivalEvent.getOrganization().getOrgName() : null);

        return dto;
    }
}
