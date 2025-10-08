package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.model.event.CompetitionEvent;
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
    public List<CompetitionEventDetailDTO> getAllCompetitionEvents() {
        return competitionEventRepo.findAll().stream().map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CompetitionEventDetailDTO getCompetitionEventById(Long id) {
        System.out.println("DEBUG: Looking for CompetitionEvent with id: " + id);
        
        // Debug: List all competition events
        System.out.println("DEBUG: All CompetitionEvents in DB:");
        competitionEventRepo.findAll().forEach(e -> System.out.println("  - ID: " + e.getId() + ", Title: " + e.getTitle()));
        
        CompetitionEvent event = competitionEventRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Competition Event not found with id " + id));
        return mapToDTO(event);
    }

    // Hàm riêng để chuyển Entity -> DTO
    private CompetitionEventDetailDTO mapToDTO(CompetitionEvent e) {
        if (e == null) return null;

        // 1️⃣ Khởi tạo DTO cơ bản (các field chính của sự kiện)
        CompetitionEventDetailDTO dto = new CompetitionEventDetailDTO();
        // 🧩 1️⃣ Thông tin cơ bản
        dto.setDescription(e.getDescription());
        dto.setTitle(e.getTitle());
        dto.setCapacity(e.getCapacity());
        dto.setStartsAt(e.getStartsAt());
        dto.setEndsAt(e.getEndsAt());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setEventType(e.getEventType());
        dto.setBenefits(e.getBenefits());
        dto.setVenueAddress(e.getVenueAddress());
        dto.setGuidelines(e.getGuidelines());

        // 2️⃣ Ảnh sự kiện (EventImages → imageUrls)
        if (e.getEventImages() != null && !e.getEventImages().isEmpty()) {
            List<String> imageUrls = e.getEventImages().stream()
                    .sorted(Comparator.comparing(EventImage::getOrderIndex)) // Sắp theo thứ tự
                    .map(EventImage::getUrl)
                    .collect(Collectors.toList());
            dto.setImageUrls(imageUrls); // ⚡ cần có trường này trong DTO
        }

        // 3️⃣ Diễn giả (Speakers → SpeakerDTO)
        if (e.getSpeakers() != null && !e.getSpeakers().isEmpty()) {
            dto.setSpeakers(
                    e.getSpeakers().stream()
                            .distinct()
                            .map(sp -> new SpeakerDTO(
                                    sp.getName(),
                                    sp.getDefaultRole() != null ? sp.getDefaultRole().name() : "Speaker",
                                    sp.getImageUrl(),
                                    sp.getProfile()
                            ))
                            .collect(Collectors.toList())
            );
        }

        // 4️⃣ Lịch trình (Schedules → ScheduleDTO)
        if (e.getSchedules() != null && !e.getSchedules().isEmpty()) {
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

        // 5️⃣ Địa điểm (Places → PlaceDTO)
        if (e.getPlaces() != null && !e.getPlaces().isEmpty()) {
            dto.setPlaces(
                    e.getPlaces().stream()
                            .map(p -> new PlaceDTO(
                                    p.getPlaceName(),
                                    p.getBuilding() != null ? p.getBuilding().name() : ""
                            ))
                            .collect(Collectors.toList())
            );
        }

        // 6️⃣ Thông tin địa điểm & hướng dẫn thêm
        dto.setVenueAddress(e.getVenueAddress());
        dto.setGuidelines(e.getGuidelines());
        dto.setEligibility(e.getEligibility());
        dto.setFormat(e.getFormat());
        dto.setJudgingCriteria(e.getJudgingCriteria());

        return dto;
    }

}
