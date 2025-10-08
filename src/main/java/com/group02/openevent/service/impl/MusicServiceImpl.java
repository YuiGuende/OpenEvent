package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.dto.event.MusicEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.service.IMusicService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MusicServiceImpl implements IMusicService {

    private final IMusicEventRepo musicEventRepo;

    public MusicServiceImpl(IMusicEventRepo musicEventRepo) {
        this.musicEventRepo = musicEventRepo;
    }

    @Override
    public MusicEventDetailDTO getMusicEventById(Long id) {
        return musicEventRepo.findById(id)
                .map(e -> {
                    // Tạo DTO cơ bản
                    MusicEventDetailDTO dto = new MusicEventDetailDTO(
                            e.getDescription(),
                            e.getTitle(),
                            e.getCapacity(),
                            e.getStartsAt(),
                            e.getEndsAt(),
                            e.getCreatedAt(),
                            null,//chưa có trường dữ liệu, không được sửa
                            e.getEventType(),
                            e.getBenefits(),
                            null, // imageUrls sẽ set sau
                            null, // speakers sẽ set sau
                            null, // schedules sẽ set sau
                            null, // places sẽ set sau
                            e.getVenueAddress(), // venue address
                            e.getGuidelines()    // guidelines
                    );

                    // Capacity đã được set trong constructor

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
                .orElseThrow(() -> new RuntimeException("Music Event not found"));
    }
    @Override
    public List<MusicEventDetailDTO> getAllMusicEvents() {
        return musicEventRepo.findAll()
                .stream()
                .map(e -> getMusicEventById(e.getId())) // tái sử dụng mapping trên
                .collect(Collectors.toList());
    }

    @Override
    public List<EventImage> getEventImages(Long eventId) {
        return musicEventRepo.findById(eventId)
                .map(event -> event.getEventImages().stream()
                        .sorted(Comparator.comparing(EventImage::getOrderIndex))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }
}
