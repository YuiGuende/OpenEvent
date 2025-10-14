package com.group02.openevent.service.impl;

import com.group02.openevent.dto.*;
import com.group02.openevent.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.ICompetitionEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CompetitionServiceImpl implements ICompetitionService {

    private final ICompetitionEventRepo competitionEventRepo;
    private final ITicketTypeRepo ticketTypeRepo;

    public CompetitionServiceImpl(ICompetitionEventRepo competitionEventRepo, ITicketTypeRepo ticketTypeRepo) {
        this.competitionEventRepo = competitionEventRepo;
        this.ticketTypeRepo = ticketTypeRepo;
    }

    @Override
    public CompetitionEventDetailDTO getCompetitionEventById(Long id) {
        return competitionEventRepo.findById(id)
                .map(competitionEvent -> {
                    CompetitionEventDetailDTO dto = new CompetitionEventDetailDTO();
                    dto.setId(competitionEvent.getId());
                    // === 1. Mapping các trường chung từ Event (lớp cha) ===
                    dto.setTitle(competitionEvent.getTitle());
                    dto.setDescription(competitionEvent.getDescription());
                    dto.setCapacity(competitionEvent.getCapacity());
                    dto.setStartsAt(competitionEvent.getStartsAt());
                    dto.setEndsAt(competitionEvent.getEndsAt());
                    dto.setCreatedAt(competitionEvent.getCreatedAt());
                    dto.setEventType(competitionEvent.getEventType());
                    dto.setBenefits(competitionEvent.getBenefits());
                    dto.setVenueAddress(competitionEvent.getVenueAddress());
                    dto.setGuidelines(competitionEvent.getGuidelines());
                    dto.setEnrollDeadline(competitionEvent.getEnrollDeadline());
                    dto.setStatus(competitionEvent.getStatus());

                    // === 2. Mapping các trường hình ảnh (Banner và Gallery) ===
                    dto.setBannerUrl(competitionEvent.getImageUrl());
                    if (competitionEvent.getEventImages() != null && !competitionEvent.getEventImages().isEmpty()) {
                        dto.setGalleryUrls(competitionEvent.getEventImages().stream()
                                .map(EventImage::getUrl).collect(Collectors.toList()));
                    } else {
                        dto.setGalleryUrls(Collections.emptyList());
                    }

                    // ⭐ === 3. Mapping các trường đặc thù của CompetitionEvent (lớp con) ===
                    dto.setCompetitionType(competitionEvent.getCompetitionType());
                    dto.setRules(competitionEvent.getRules());
                    dto.setPrizePool(competitionEvent.getPrizePool());
                    dto.setEligibility(competitionEvent.getEligibility());
                    dto.setFormat(competitionEvent.getFormat());
                    dto.setJudgingCriteria(competitionEvent.getJudgingCriteria());

                    // === 4. Mapping các đối tượng và danh sách liên quan ===
                    // 🔹 Speakers
                    if (competitionEvent.getSpeakers() != null) {
                        dto.setSpeakers(competitionEvent.getSpeakers().stream().distinct()
                                .map(sp -> new SpeakerDTO(sp.getName(), sp.getRole(), sp.getImageUrl(), sp.getProfile()))
                                .collect(Collectors.toList()));
                    }

                    // 🔹 Schedules
                    if (competitionEvent.getSchedules() != null) {
                        List<ScheduleDTO> scheduleList = competitionEvent.getSchedules().stream()
                                .map(sc -> new ScheduleDTO(sc.getActivity(), sc.getStartTime(), sc.getEndTime()))
                                .collect(Collectors.toList());
                        dto.setSchedules(scheduleList);
                        dto.setSchedulesByDay(scheduleList.stream()
                                .collect(Collectors.groupingBy(schedule -> schedule.getStartTime().toLocalDate())));
                    }

                    // 🔹 Places
                    if (competitionEvent.getPlaces() != null) {
                        dto.setPlaces(competitionEvent.getPlaces().stream()
                                .map(p -> new PlaceDTO(p.getPlaceName(), p.getBuilding().name()))
                                .collect(Collectors.toList()));
                    }

                    // 🔹 Organization
                    if (competitionEvent.getOrganization() != null) {
                        var org = competitionEvent.getOrganization();
                        dto.setOrganization(new OrganizationDTO(org.getOrgId(), org.getOrgName(), org.getDescription(), org.getWebsite(), org.getEmail(), org.getPhone(), org.getAddress(), org.getImageUrl()));
                    }

                    // 🔹 TicketTypes
                    List<TicketTypeDTO> ticketDTOs = ticketTypeRepo.findByEventId(competitionEvent.getId()).stream()
                            .map(ticket -> new TicketTypeDTO(ticket.getTicketTypeId(), ticket.getEvent().getId(), ticket.getEvent().getTitle(), ticket.getEvent().getImageUrl(), ticket.getName(), ticket.getDescription(), ticket.getPrice(), ticket.getSale(), ticket.getFinalPrice(), ticket.getTotalQuantity(), ticket.getSoldQuantity(), ticket.getAvailableQuantity(), ticket.getStartSaleDate(), ticket.getEndSaleDate(), ticket.isSalePeriodActive(), ticket.isSalePeriodActive(), !ticket.isAvailable()))
                            .collect(Collectors.toList());
                    dto.setTicketTypes(ticketDTOs);

                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Competition Event not found with id: " + id));
    }

    @Override
    public List<CompetitionEventDetailDTO> getAllCompetitionEvents() {
        return competitionEventRepo.findAll().stream()
                .map(event -> this.getCompetitionEventById(event.getId()))
                .collect(Collectors.toList());
    }
}