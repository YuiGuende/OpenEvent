package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.dto.event.ConferenceEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.IConferenceEventRepo;
import com.group02.openevent.service.IConferenceService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConferenceServiceImpl implements IConferenceService {

    private final IConferenceEventRepo conferenceEventRepo;

    public ConferenceServiceImpl(IConferenceEventRepo conferenceEventRepo) {
        this.conferenceEventRepo = conferenceEventRepo;
    }

    @Override
    public ConferenceEventDetailDTO getConferenceEventById(Long id) {
        return conferenceEventRepo.findById(id)
                .map(e -> {
                    // Tạo DTO
                    ConferenceEventDetailDTO dto = new ConferenceEventDetailDTO(
                            e.getDescription(),
                            e.getTitle(),
                            e.getCapacity(),
                            e.getStartsAt(),
                            e.getEndsAt(),
                            e.getCreatedAt(),
                            e.getEventType(),
                            e.getBenefits(),
                            null, // imageUrls
                            null, // speakers
                            null, // schedules
                            null, // places
                            e.getVenueAddress(),
                            e.getGuidelines(),
                            // Các trường của Conference
                            e.getConferenceType(),
                            e.getMaxAttendees(),
                            e.getAgenda()
                    );

                    // Map các danh sách liên quan
                    if (e.getEventImages() != null) {
                        dto.setImageUrls(e.getEventImages().stream()
                                .sorted(Comparator.comparing(EventImage::getOrderIndex))
                                .map(EventImage::getUrl)
                                .collect(Collectors.toList()));
                    }
                    if (e.getSpeakers() != null) {
                        dto.setSpeakers(e.getSpeakers().stream().distinct()
                                .map(sp -> new SpeakerDTO(sp.getName(), sp.getDefaultRole().name(), sp.getImageUrl(), sp.getProfile()))
                                .collect(Collectors.toList()));
                    }
                    if (e.getSchedules() != null) {
                        dto.setSchedules(e.getSchedules().stream()
                                .map(sc -> new ScheduleDTO(sc.getActivity(), sc.getStartTime(), sc.getEndTime()))
                                .collect(Collectors.toList()));
                    }
                    if (e.getPlaces() != null) {
                        dto.setPlaces(e.getPlaces().stream()
                                .map(p -> new PlaceDTO(p.getPlaceName(), p.getBuilding().name()))
                                .collect(Collectors.toList()));
                    }

                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Conference Event not found with id: " + id));
    }

    @Override
    public List<ConferenceEventDetailDTO> getAllConferenceEvents() {
        return conferenceEventRepo.findAll().stream()
                .map(e -> getConferenceEventById(e.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventImage> getEventImages(Long eventId) {
        return conferenceEventRepo.findById(eventId)
                .map(event -> event.getEventImages().stream()
                        .sorted(Comparator.comparing(EventImage::getOrderIndex))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }
}