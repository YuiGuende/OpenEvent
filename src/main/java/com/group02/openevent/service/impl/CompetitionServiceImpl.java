package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.dto.competition.CompetitionEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.ICompetitionEventRepo;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompetitionServiceImpl implements ICompetitionService {

    private final ICompetitionEventRepo competitionEventRepo;

    public CompetitionServiceImpl(ICompetitionEventRepo competitionEventRepo) {
        this.competitionEventRepo = competitionEventRepo;
    }

    @Override
    public CompetitionEventDetailDTO getCompetitionEventById(Integer id) {
        return competitionEventRepo.findById(id)
                .map(e -> {
                    // Tạo DTO cơ bản
                    CompetitionEventDetailDTO dto = new CompetitionEventDetailDTO(
                            e.getDescription(),
                            e.getTitle(),
                            e.getStartsAt(),
                            e.getEndsAt(),
                            e.getCreatedAt(),
                            null, // chưa có trường dữ liệu, không được sửa
                            e.getEventType(),
                            e.getBenefits()
                    );

                    // 🔹 Capacity
                    dto.setCapacity(e.getCapacity());

                    // 🔹 Competition specific fields
                    dto.setPrize(e.getPrize());
                    dto.setRules(e.getRules());
                    dto.setRequirements(e.getRequirements());
                    dto.setRegistrationFee(e.getRegistrationFee());
                    dto.setMaxParticipants(e.getMaxParticipants());

                    // 🔹 Images
                    if (e.getEventImages() != null) {
                        dto.setImageUrls(
                                e.getEventImages().stream()
                                        .sorted(Comparator.comparing(EventImage::getOrderIndex))
                                        .map(EventImage::getUrl)
                                        .collect(Collectors.toList())
                        );
                    }

                    // 🔹 Speakers (Judges, MCs)
                    if (e.getSpeakers() != null) {
                        dto.setSpeakers(
                                e.getSpeakers().stream()
                                        .distinct() // 🔑 loại trùng lặp
                                        .map(sp -> new SpeakerDTO(
                                                sp.getName(),
                                                sp.getDefaultRole().name(),
                                                sp.getImageUrl(),
                                                sp.getProfile()
                                        ))
                                        .collect(Collectors.toList())
                        );
                    }

                    // 🔹 Schedules (Competition rounds)
                    if (e.getSchedules() != null) {
                        dto.setSchedules(
                                e.getSchedules().stream()
                                        .map(sc -> new ScheduleDTO(
                                                sc.getActivity(),
                                                sc.getStartTime(),
                                                sc.getEndTime()
                                        ))
                                        .collect(Collectors.toList())
                        );
                    }

                    // 🔹 Places (Competition venues)
                    if (e.getPlaces() != null) {
                        dto.setPlaces(
                                e.getPlaces().stream()
                                        .map(p -> new PlaceDTO(
                                                p.getPlaceName(),
                                                p.getBuilding().name()
                                        ))
                                        .collect(Collectors.toList())
                        );
                    }

                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Competition Event not found"));
    }

    @Override
    public List<CompetitionEventDetailDTO> getAllCompetitionEvents() {
        return competitionEventRepo.findAll()
                .stream()
                .map(e -> getCompetitionEventById(e.getId())) // tái sử dụng mapping trên
                .collect(Collectors.toList());
    }

    @Override
    public List<EventImage> getEventImages(Integer eventId) {
        return competitionEventRepo.findById(eventId)
                .map(event -> event.getEventImages().stream()
                        .sorted(Comparator.comparing(EventImage::getOrderIndex))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }
}
