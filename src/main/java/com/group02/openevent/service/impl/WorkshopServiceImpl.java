package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.dto.event.WorkshopEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.IWorkshopEventRepo;
import com.group02.openevent.service.IWorkshopService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkshopServiceImpl implements IWorkshopService {

    private final IWorkshopEventRepo workshopEventRepo;

    public WorkshopServiceImpl(IWorkshopEventRepo workshopEventRepo) {
        this.workshopEventRepo = workshopEventRepo;
    }

    @Override
    public WorkshopEventDetailDTO getWorkshopEventById(Long id) {
        return workshopEventRepo.findById(id)
                .map(e -> {
                    // Tạo DTO cơ bản với các trường chung và trường riêng của Workshop
                    WorkshopEventDetailDTO dto = new WorkshopEventDetailDTO(
                            e.getDescription(),
                            e.getTitle(),
                            e.getCapacity(),
                            e.getStartsAt(),
                            e.getEndsAt(),
                            e.getCreatedAt(),
                            null, // updatedAt chưa có trường dữ liệu
                            e.getEventType(),
                            e.getBenefits(),
                            null, // imageUrls sẽ set sau
                            null, // speakers sẽ set sau
                            null, // schedules sẽ set sau
                            null, // places sẽ set sau
                            e.getVenueAddress(),
                            e.getGuidelines(),
                            // Các trường của Workshop
                            e.getTopic(),
                            e.getMaterialsLink(),
                            e.getMaxParticipants(),
                            e.getSkillLevel(),
                            e.getPrerequisites()
                    );

                    // 🔹 Images
                    if (e.getEventImages() != null) {
                        dto.setImageUrls(
                                e.getEventImages().stream()
                                        .sorted(Comparator.comparing(EventImage::getOrderIndex))
                                        .map(EventImage::getUrl)
                                        .collect(Collectors.toList())
                        );
                    }

                    // 🔹 Speakers
                    if (e.getSpeakers() != null) {
                        dto.setSpeakers(
                                e.getSpeakers().stream()
                                        .distinct()
                                        .map(sp -> new SpeakerDTO(
                                                sp.getName(),
                                                sp.getDefaultRole().name(),
                                                sp.getImageUrl(),
                                                sp.getProfile()
                                        ))
                                        .collect(Collectors.toList())
                        );
                    }

                    // 🔹 Schedules
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

                    // 🔹 Places
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
                .orElseThrow(() -> new RuntimeException("Workshop Event not found with id: " + id));
    }

    @Override
    public List<WorkshopEventDetailDTO> getAllWorkshopEvents() {
        return workshopEventRepo.findAll()
                .stream()
                .map(e -> getWorkshopEventById(e.getId())) // Tái sử dụng logic mapping
                .collect(Collectors.toList());
    }

    @Override
    public List<EventImage> getEventImages(Long eventId) {
        return workshopEventRepo.findById(eventId)
                .map(event -> event.getEventImages().stream()
                        .sorted(Comparator.comparing(EventImage::getOrderIndex))
                        .collect(Collectors.toList()))
                .orElse(List.of()); // Trả về danh sách rỗng nếu không tìm thấy
    }
}