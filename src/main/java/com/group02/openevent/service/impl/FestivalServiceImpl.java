package com.group02.openevent.service.impl;

import com.group02.openevent.dto.*;
import com.group02.openevent.dto.event.FestivalEventDetailDTO;
import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.IFestivalEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.IFestivalService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FestivalServiceImpl implements IFestivalService {

    private final IFestivalEventRepo festivalEventRepo;
    private final ITicketTypeRepo ticketTypeRepo;

    public FestivalServiceImpl(IFestivalEventRepo festivalEventRepo, ITicketTypeRepo ticketTypeRepo) {
        this.festivalEventRepo = festivalEventRepo;
        this.ticketTypeRepo = ticketTypeRepo;
    }

    @Override
    public FestivalEventDetailDTO getFestivalEventById(Long id) {
        return festivalEventRepo.findById(id)
                .map(festivalEvent -> {
                    FestivalEventDetailDTO dto = new FestivalEventDetailDTO();

                    // === 1. Mapping các trường chung từ Event (lớp cha) ===
                    dto.setId(festivalEvent.getId());
                    dto.setTitle(festivalEvent.getTitle());
                    dto.setDescription(festivalEvent.getDescription());
                    dto.setCapacity(festivalEvent.getCapacity());
                    dto.setStartsAt(festivalEvent.getStartsAt());
                    dto.setEndsAt(festivalEvent.getEndsAt());
                    dto.setCreatedAt(festivalEvent.getCreatedAt());
                    dto.setEventType(festivalEvent.getEventType());
                    dto.setBenefits(festivalEvent.getBenefits());
                    dto.setVenueAddress(festivalEvent.getVenueAddress());
                    dto.setGuidelines(festivalEvent.getGuidelines());
                    dto.setEnrollDeadline(festivalEvent.getEnrollDeadline());
                    dto.setStatus(festivalEvent.getStatus());

                    // === 2. Mapping các trường hình ảnh (Banner và Gallery) ===
                    dto.setBannerUrl(festivalEvent.getImageUrl());
                    if (festivalEvent.getEventImages() != null && !festivalEvent.getEventImages().isEmpty()) {
                        dto.setGalleryUrls(festivalEvent.getEventImages().stream()
                                .map(EventImage::getUrl).collect(Collectors.toList()));
                    } else {
                        dto.setGalleryUrls(Collections.emptyList());
                    }

                    // ⭐ === 3. Mapping các trường đặc thù của FestivalEvent (lớp con) ===
                    dto.setCulture(festivalEvent.getCulture());
                    dto.setHighlight(festivalEvent.getHighlight());
                    dto.setFestivalTheme(festivalEvent.getFestivalTheme());
                    dto.setNumberOfActivities(festivalEvent.getNumberOfActivities());
                    dto.setTargetAudience(festivalEvent.getTargetAudience());
                    dto.setRegistrationRequired(festivalEvent.getRegistrationRequired());
                    dto.setFestivalType(festivalEvent.getFestivalType());
                    dto.setMainOrganizer(festivalEvent.getMainOrganizer());

                    // === 4. Mapping các đối tượng và danh sách liên quan ===
                    // 🔹 Speakers
                    if (festivalEvent.getSpeakers() != null) {
                        dto.setSpeakers(festivalEvent.getSpeakers().stream().distinct()
                                .map(sp -> new SpeakerDTO(sp.getName(), sp.getRole(), sp.getImageUrl(), sp.getProfile()))
                                .collect(Collectors.toList()));
                    }

                    // 🔹 Schedules
                    if (festivalEvent.getSchedules() != null) {
                        List<ScheduleDTO> scheduleList = festivalEvent.getSchedules().stream()
                                .map(sc -> new ScheduleDTO(sc.getActivity(), sc.getStartTime(), sc.getEndTime()))
                                .collect(Collectors.toList());
                        dto.setSchedules(scheduleList);
                        dto.setSchedulesByDay(scheduleList.stream()
                                .collect(Collectors.groupingBy(schedule -> schedule.getStartTime().toLocalDate())));
                    }

                    // 🔹 Places
                    if (festivalEvent.getPlaces() != null) {
                        dto.setPlaces(festivalEvent.getPlaces().stream()
                                .map(p -> new PlaceDTO(p.getPlaceName(), p.getBuilding().name()))
                                .collect(Collectors.toList()));
                    }

                    // 🔹 Organization
                    if (festivalEvent.getOrganization() != null) {
                        var org = festivalEvent.getOrganization();
                        dto.setOrganization(new OrganizationDTO(org.getOrgId(), org.getOrgName(), org.getDescription(), org.getWebsite(), org.getEmail(), org.getPhone(), org.getAddress(), org.getImageUrl()));
                    }

                    // 🔹 TicketTypes
                    List<TicketTypeDTO> ticketDTOs = ticketTypeRepo.findByEventId(festivalEvent.getId()).stream()
                            .map(ticket -> {
                                LocalDateTime now = LocalDateTime.now();
                                boolean isSaleActive = ticket.isSalePeriodActive();
                                boolean saleNotStarted = ticket.getStartSaleDate() != null && now.isBefore(ticket.getStartSaleDate());
                                boolean saleOverdue = ticket.getEndSaleDate() != null && now.isAfter(ticket.getEndSaleDate());
                                boolean isSoldOut = ticket.getAvailableQuantity() <= 0;
                                String countdown = null;
                                if (saleNotStarted) {
                                    long days = Duration.between(now, ticket.getStartSaleDate()).toDays();
                                    if (days < 0) days = 0;
                                    countdown = days + (days == 1 ? " day" : " days");
                                }
                                return TicketTypeDTO.builder()
                                        .ticketTypeId(ticket.getTicketTypeId())
                                        .eventId(ticket.getEvent().getId())
                                        .eventTitle(ticket.getEvent().getTitle())
                                        .eventImageUrl(ticket.getEvent().getImageUrl())
                                        .name(ticket.getName())
                                        .description(ticket.getDescription())
                                        .price(ticket.getPrice())
                                        .sale(ticket.getSale() != null ? ticket.getSale() : BigDecimal.ZERO)
                                        .finalPrice(ticket.getFinalPrice())
                                        .totalQuantity(ticket.getTotalQuantity())
                                        .soldQuantity(ticket.getSoldQuantity())
                                        .availableQuantity(ticket.getAvailableQuantity())
                                        .startSaleDate(ticket.getStartSaleDate())
                                        .endSaleDate(ticket.getEndSaleDate())
                                        .isAvailable(ticket.getAvailableQuantity() > 0
                                                && (ticket.getStartSaleDate() == null || now.isAfter(ticket.getStartSaleDate()))
                                                && (ticket.getEndSaleDate() == null || now.isBefore(ticket.getEndSaleDate())))
                                        .isSaleActive(isSaleActive)
                                        .isSoldOut(isSoldOut)
                                        .saleNotStarted(saleNotStarted)
                                        .saleStartCountdownText(countdown)
                                        .saleOverdue(saleOverdue)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    dto.setTicketTypes(ticketDTOs);

                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Festival Event not found with id: " + id));
    }

    @Override
    public List<FestivalEventDetailDTO> getAllFestivalEvents() {
        return festivalEventRepo.findAll().stream()
                .map(event -> this.getFestivalEventById(event.getId()))
                .collect(Collectors.toList());
    }
}