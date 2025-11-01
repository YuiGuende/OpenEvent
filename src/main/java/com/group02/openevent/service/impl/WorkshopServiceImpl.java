package com.group02.openevent.service.impl;

import com.group02.openevent.dto.*;
import com.group02.openevent.dto.event.WorkshopEventDetailDTO;
import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.IWorkshopEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.IWorkshopService;
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
public class WorkshopServiceImpl implements IWorkshopService {

    private final IWorkshopEventRepo workshopEventRepo;
    private final ITicketTypeRepo ticketTypeRepo;

    public WorkshopServiceImpl(IWorkshopEventRepo workshopEventRepo, ITicketTypeRepo ticketTypeRepo) {
        this.workshopEventRepo = workshopEventRepo;
        this.ticketTypeRepo = ticketTypeRepo;
    }

    @Override
    public WorkshopEventDetailDTO getWorkshopEventById(Long id) {
        return workshopEventRepo.findById(id)
                .map(workshopEvent -> {
                    WorkshopEventDetailDTO dto = new WorkshopEventDetailDTO();

                    // === 1. Mapping các trường chung từ Event (lớp cha) ===
                    dto.setId(workshopEvent.getId());
                    dto.setTitle(workshopEvent.getTitle());
                    dto.setDescription(workshopEvent.getDescription());
                    dto.setCapacity(workshopEvent.getCapacity());
                    dto.setStartsAt(workshopEvent.getStartsAt());
                    dto.setEndsAt(workshopEvent.getEndsAt());
                    dto.setCreatedAt(workshopEvent.getCreatedAt());
                    dto.setEventType(workshopEvent.getEventType());
                    dto.setBenefits(workshopEvent.getBenefits());
                    dto.setVenueAddress(workshopEvent.getVenueAddress());
                    dto.setGuidelines(workshopEvent.getGuidelines());
                    dto.setEnrollDeadline(workshopEvent.getEnrollDeadline());
                    dto.setStatus(workshopEvent.getStatus());

                    // === 2. Mapping các trường hình ảnh (Banner và Gallery) ===
                    dto.setBannerUrl(workshopEvent.getImageUrl());
                    if (workshopEvent.getEventImages() != null && !workshopEvent.getEventImages().isEmpty()) {
                        dto.setGalleryUrls(workshopEvent.getEventImages().stream()
                                .map(EventImage::getUrl).collect(Collectors.toList()));
                    } else {
                        dto.setGalleryUrls(Collections.emptyList());
                    }

                    // ⭐ === 3. Mapping các trường đặc thù của WorkshopEvent (lớp con) ===
                    dto.setTopic(workshopEvent.getTopic());
                    dto.setMaterialsLink(workshopEvent.getMaterialsLink());
                    dto.setMaxParticipants(workshopEvent.getMaxParticipants());
                    dto.setSkillLevel(workshopEvent.getSkillLevel());
                    dto.setPrerequisites(workshopEvent.getPrerequisites());

                    // === 4. Mapping các đối tượng và danh sách liên quan ===
                    // 🔹 Speakers
                    if (workshopEvent.getSpeakers() != null) {
                        dto.setSpeakers(workshopEvent.getSpeakers().stream().distinct()
                                .map(sp -> new SpeakerDTO(sp.getName(), sp.getRole(), sp.getImageUrl(), sp.getProfile()))
                                .collect(Collectors.toList()));
                    }

                    // 🔹 Schedules
                    if (workshopEvent.getSchedules() != null) {
                        List<ScheduleDTO> scheduleList = workshopEvent.getSchedules().stream()
                                .map(sc -> new ScheduleDTO(sc.getActivity(), sc.getStartTime(), sc.getEndTime()))
                                .collect(Collectors.toList());
                        dto.setSchedules(scheduleList);
                        dto.setSchedulesByDay(scheduleList.stream()
                                .collect(Collectors.groupingBy(schedule -> schedule.getStartTime().toLocalDate())));
                    }

                    // 🔹 Places
                    if (workshopEvent.getPlaces() != null) {
                        dto.setPlaces(workshopEvent.getPlaces().stream()
                                .map(p -> new PlaceDTO(p.getPlaceName(), p.getBuilding().name()))
                                .collect(Collectors.toList()));
                    }

                    // 🔹 Organization
                    if (workshopEvent.getOrganization() != null) {
                        var org = workshopEvent.getOrganization();
                        dto.setOrganization(new OrganizationDTO(org.getOrgId(), org.getOrgName(), org.getDescription(), org.getWebsite(), org.getEmail(), org.getPhone(), org.getAddress(), org.getImageUrl()));
                    }

                    // 🔹 TicketTypes
                    List<TicketTypeDTO> ticketDTOs = ticketTypeRepo.findByEventId(workshopEvent.getId()).stream()
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
                .orElseThrow(() -> new RuntimeException("Workshop Event not found with id: " + id));
    }

    @Override
    public List<WorkshopEventDetailDTO> getAllWorkshopEvents() {
        return workshopEventRepo.findAll().stream()
                // 👇 SỬA LỖI TẠI ĐÂY: Dùng lambda expression thay vì method reference
                .map(event -> this.getWorkshopEventById(event.getId()))
                .collect(Collectors.toList());
    }
}