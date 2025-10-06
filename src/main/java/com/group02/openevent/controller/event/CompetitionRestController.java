package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.competition.CompetitionEventDetailDTO;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.CompetitionEvent;
import com.group02.openevent.repository.ICompetitionEventRepo;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/competition")
public class CompetitionRestController {
    private final ICompetitionEventRepo competitionEventRepo;
    private final ICompetitionService competitionService;

    public CompetitionRestController(ICompetitionEventRepo competitionEventRepo, ICompetitionService competitionService) {
        this.competitionEventRepo = competitionEventRepo;
        this.competitionService = competitionService;
    }

    // Lấy tất cả competition events
    @GetMapping
    public List<CompetitionEventDetailDTO> getAllCompetitions() {
        return competitionService.getAllCompetitionEvents();
    }

    // Lấy chi tiết 1 competition theo id
    @GetMapping("/{id}")
    public CompetitionEventDetailDTO getCompetitionById(@PathVariable Long id) {
        return competitionService.getCompetitionEventById(id);
    }


    @PostMapping
    public ResponseEntity<?> createCompetition(@RequestBody CompetitionEventDetailDTO dto) {
        try {
            // Validate input
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title không được để trống"));
            }
            if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Description không được để trống"));
            }
            if (dto.getStartsAt() == null || dto.getEndsAt() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Start time và End time không được để trống"));
            }
            if (dto.getStartsAt().isAfter(dto.getEndsAt())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Start time phải trước End time"));
            }

            // Chuyển DTO thành Entity để lưu DB
            CompetitionEvent event = new CompetitionEvent();
            event.setTitle(dto.getTitle());
            event.setDescription(dto.getDescription());
            event.setCapacity(dto.getCapacity());
            event.setStartsAt(dto.getStartsAt());
            event.setEndsAt(dto.getEndsAt());
            event.setBenefits(dto.getBenefits());
            event.setEventType(dto.getEventType());
            event.setVenueAddress(dto.getVenueAddress());
            event.setGuidelines(dto.getGuidelines());
            event.setCreatedAt(LocalDateTime.now());
            event.setStatus(EventStatus.DRAFT);
            event.setEnrollDeadline(LocalDateTime.now().plusDays(7));
            
            CompetitionEvent saved = competitionEventRepo.save(event);

            // Trả lại dữ liệu vừa tạo
            CompetitionEventDetailDTO response = new CompetitionEventDetailDTO(
                    saved.getDescription(),
                    saved.getTitle(),
                    saved.getCapacity(),
                    saved.getStartsAt(),
                    saved.getEndsAt(),
                    saved.getCreatedAt(),
                    saved.getEventType(),
                    saved.getBenefits(),
                    dto.getImageUrls(), // giữ nguyên từ request
                    dto.getSpeakers(),  // giữ nguyên từ request
                    dto.getSchedules(), // giữ nguyên từ request
                    dto.getPlaces(),    // giữ nguyên từ request
                    saved.getVenueAddress(),
                    saved.getGuidelines()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.err.println("Error creating competition: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."));
        }
    }

}
