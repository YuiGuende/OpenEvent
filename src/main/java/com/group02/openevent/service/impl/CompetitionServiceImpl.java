package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.*;
import com.group02.openevent.model.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.ICompetitionEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
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

                    // === Mapping các trường chung từ Event ===
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
                    dto.setBannerUrl(competitionEvent.getImageUrl());

                    if (competitionEvent.getEventImages() != null && !competitionEvent.getEventImages().isEmpty()) {
                        dto.setGalleryUrls(competitionEvent.getEventImages().stream()
                                .map(EventImage::getUrl).collect(Collectors.toList()));
                    } else {
                        dto.setGalleryUrls(Collections.emptyList());
                    }

                    // === Mapping các trường đặc thù của CompetitionEvent ===
                    dto.setCompetitionType(competitionEvent.getCompetitionType());
                    dto.setRules(competitionEvent.getRules());
                    dto.setPrizePool(competitionEvent.getPrizePool());
                    dto.setEligibility(competitionEvent.getEligibility());
                    dto.setFormat(competitionEvent.getFormat());
                    dto.setJudgingCriteria(competitionEvent.getJudgingCriteria());

                    // === Mapping các danh sách và đối tượng liên quan (AN TOÀN HƠN) ===

                    // ⭐ THÊM KIỂM TRA NULL CHO SPEAKERS
                    if (competitionEvent.getSpeakers() != null) {
                        dto.setSpeakers(competitionEvent.getSpeakers().stream().distinct()
                                .map(sp -> new SpeakerDTO(sp.getName(), sp.getRole(), sp.getImageUrl(), sp.getProfile()))
                                .collect(Collectors.toList()));
                    }

                    // ⭐ THÊM KIỂM TRA NULL CHO SCHEDULES
                    if (competitionEvent.getSchedules() != null) {
                        dto.setSchedules(competitionEvent.getSchedules().stream()
                                .map(sc -> new ScheduleDTO(sc.getActivity(), sc.getStartTime(), sc.getEndTime()))
                                .collect(Collectors.toList()));
                    }

                    // ⭐ THÊM KIỂM TRA NULL CHO PLACES
                    if (competitionEvent.getPlaces() != null) {
                        dto.setPlaces(competitionEvent.getPlaces().stream()
                                .map(p -> new PlaceDTO(p.getPlaceName(), p.getBuilding().name()))
                                .collect(Collectors.toList()));
                    }

                    // ⭐ THÊM KIỂM TRA NULL CHO ORGANIZATION (QUAN TRỌNG NHẤT)
                    if (competitionEvent.getOrganization() != null) {
                        var org = competitionEvent.getOrganization();
                        dto.setOrganization(new OrganizationDTO(org.getOrgId(), org.getOrgName(), org.getDescription(), org.getWebsite(), org.getEmail(), org.getPhone(), org.getAddress(), org.getImageUrl()));
                    }

                    // Mapping TicketTypes (vốn đã an toàn vì findByEventId sẽ trả về list rỗng nếu không có)
                    List<TicketTypeDTO> ticketDTOs = ticketTypeRepo.findByEventId(competitionEvent.getId()).stream()
                            .map(ticket -> new TicketTypeDTO(ticket.getTicketTypeId(), ticket.getName(), ticket.getDescription(), ticket.getPrice(), ticket.getAvailableQuantity(), !ticket.isAvailable(), ticket.getStartSaleDate(), ticket.getEndSaleDate(), ticket.isSalePeriodActive()))
                            .collect(Collectors.toList());
                    dto.setTicketTypes(ticketDTOs);

                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Competition Event with ID " + id + " not found in database."));
    }

    @Override
    public List<CompetitionEventDetailDTO> getAllCompetitionEvents() {
        return competitionEventRepo.findAll().stream()
                .map(e -> getCompetitionEventById(e.getId()))
                .collect(Collectors.toList());
    }
}