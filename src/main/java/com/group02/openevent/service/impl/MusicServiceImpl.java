package com.group02.openevent.service.impl;

import com.group02.openevent.dto.PlaceDTO;
import com.group02.openevent.dto.ScheduleDTO;
import com.group02.openevent.dto.SpeakerDTO;
import com.group02.openevent.dto.event.MusicEventDetailDTO;
import com.group02.openevent.dto.OrganizationDTO;
import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.IMusicService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.math.BigDecimal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class MusicServiceImpl implements IMusicService {
    private final IMusicEventRepo musicEventRepo;
    private final ITicketTypeRepo ticketTypeRepo; // ⭐️ THÊM MỚI: Inject Ticket Repo

    // ⭐️ CẬP NHẬT: Constructor để inject cả 2 repo
    public MusicServiceImpl(IMusicEventRepo musicEventRepo, ITicketTypeRepo ticketTypeRepo) {
        this.musicEventRepo = musicEventRepo;
        this.ticketTypeRepo = ticketTypeRepo;
    }

    @Override
    public MusicEventDetailDTO getMusicEventById(Long id) {
        // ⭐️ SỬA: Phải dùng MusicEvent để lấy được genre và musicType
        return musicEventRepo.findById(id)
                .map(musicEvent -> {
                    // Tạo DTO rỗng để set giá trị
                    MusicEventDetailDTO dto = new MusicEventDetailDTO();
                    dto.setId(musicEvent.getId());
                    // === Mapping các trường cơ bản từ Event (lớp cha) ===
                    dto.setTitle(musicEvent.getTitle());
                    dto.setDescription(musicEvent.getDescription());
                    dto.setCapacity(musicEvent.getCapacity());
                    dto.setStartsAt(musicEvent.getStartsAt());
                    dto.setEndsAt(musicEvent.getEndsAt());
                    dto.setCreatedAt(musicEvent.getCreatedAt());
                    dto.setEventType(musicEvent.getEventType());
                    dto.setBenefits(musicEvent.getBenefits());
                    dto.setVenueAddress(musicEvent.getVenueAddress());
                    dto.setGuidelines(musicEvent.getGuidelines());
                    dto.setEnrollDeadline(musicEvent.getEnrollDeadline()); // ⭐️ THÊM MỚI
                    dto.setStatus(musicEvent.getStatus());                 // ⭐️ THÊM MỚI

                    // === Mapping các trường đặc thù của MusicEvent (lớp con) ===
                    dto.setMusicType(musicEvent.getMusicType()); // ⭐️ THÊM MỚI
                    dto.setGenre(musicEvent.getGenre());         // ⭐️ THÊM MỚI

                    // ⭐️ CẬP NHẬT: Tách bạch Banner và Gallery
                    // 🔹 Banner (1 ảnh chính)
                    dto.setBannerUrl(musicEvent.getImageUrl());
                    // 🔹 Gallery (nhiều ảnh phụ)
                    if (musicEvent.getEventImages() != null && !musicEvent.getEventImages().isEmpty()) {
                        dto.setGalleryUrls(
                                musicEvent.getEventImages().stream()
                                        .map(EventImage::getUrl)
                                        .collect(Collectors.toList())
                        );
                    } else {
                        dto.setGalleryUrls(Collections.emptyList());
                    }

                    // 🔹 Speakers
                    if (musicEvent.getSpeakers() != null) {
                        dto.setSpeakers(
                                musicEvent.getSpeakers().stream()
                                        .distinct()
                                        .map(sp -> new SpeakerDTO(sp.getName(), sp.getRole(), sp.getImageUrl(), sp.getProfile()))
                                        .collect(Collectors.toList())
                        );
                    }

                    // 🔹 Schedules
                    if (musicEvent.getSchedules() != null) {
                        List<ScheduleDTO> scheduleDTOs = musicEvent.getSchedules().stream()
                                .map(sc -> new ScheduleDTO(sc.getActivity(), sc.getStartTime(), sc.getEndTime()))
                                .collect(Collectors.toList());
                        dto.setSchedules(scheduleDTOs);

                        // Group schedules by day for 2-column display
                        dto.setSchedulesByDay(
                                scheduleDTOs.stream()
                                        .collect(Collectors.groupingBy(
                                                sc -> sc.getStartTime().toLocalDate(),
                                                Collectors.toList()
                                        ))
                        );
                    }

                    // 🔹 Places
                    if (musicEvent.getPlaces() != null) {
                        dto.setPlaces(
                                musicEvent.getPlaces().stream()
                                        .map(p -> new PlaceDTO(p.getPlaceName(), p.getBuilding().name()))
                                        .collect(Collectors.toList())
                        );
                    }

                    // ⭐️ THÊM MỚI: Mapping Organization
                    if (musicEvent.getOrganization() != null) {
                        var org = musicEvent.getOrganization();
                        dto.setOrganization(new OrganizationDTO(
                                org.getOrgId(),
                                org.getOrgName(),
                                org.getDescription(),
                                org.getWebsite(),
                                org.getEmail(),
                                org.getPhone(),
                                org.getAddress(),
                                org.getImageUrl()
                        ));
                    }

                    // ⭐️ THÊM MỚI: Mapping TicketTypes
                    // Dùng ticketTypeRepo để tìm vé theo event id
                    List<TicketTypeDTO> ticketDTOs = ticketTypeRepo.findByEventId(musicEvent.getId()).stream()
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
                .orElseThrow(() -> new RuntimeException("Music Event not found with id: " + id));
    }


    @Override
    public List<MusicEventDetailDTO> getAllMusicEvents() {
        return musicEventRepo.findAll()
                .stream()
                .map(e -> getMusicEventById(e.getId())) // tái sử dụng mapping trên
                .collect(Collectors.toList());
    }
}
