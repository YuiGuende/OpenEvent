package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.IPlaceService;
import lombok.extern.slf4j.Slf4j;
import com.group02.openevent.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PlaceServiceImpl implements IPlaceService {
    @Autowired
    IPlaceRepo placeRepo;

    @Override
    public List<Place> getAllByEventId(Long id) {
        log.info("🔍 Loading places for event ID: {}", id);

        // First, check if there are any places in the database at all
        List<Place> allPlaces = placeRepo.findAll();
        log.info("🗄️ Total places in database: {}", allPlaces.size());

        // Try JPQL query first
        List<Place> places = placeRepo.findPlacesByEventId(id);
        log.info("📋 JPQL query found {} places for event ID {}: {}", places.size(), id, places);

        // If no results, try native query
        if (places.isEmpty()) {
            log.info("🔄 No results from JPQL, trying native query...");
            places = placeRepo.findPlacesByEventIdNative(id);
            log.info("📋 Native query found {} places for event ID {}: {}", places.size(), id, places);
        }

        return places;
    }

    @Override
    public Optional<Place> findPlaceById(Long placeId) {
        return placeRepo.findById(placeId);
    }

    @Override
    public Optional<Place> findPlaceByName(String placeName) {
        return placeRepo.findByPlaceName(placeName);
    }

    @Override
    public Optional<Place> findPlaceByNameFlexible(String placeName) {
        // Loại bỏ từ "tòa" và các từ không cần thiết
        String cleanedPlaceName = placeName.replaceAll("(?i)\\b(tòa|toa|building)\\b", "").trim();

        // Tìm kiếm linh hoạt với tên đã làm sạch
        List<Place> places = placeRepo.findByPlaceNameContainingIgnoreCase(cleanedPlaceName);

        if (!places.isEmpty()) {
            return Optional.of(places.get(0));
        }

        // Thử tìm kiếm với tên gốc
        places = placeRepo.findByPlaceNameContainingIgnoreCase(placeName);
        if (!places.isEmpty()) {
            return Optional.of(places.get(0));
        }

        // Thử tìm kiếm từng phần của tên
        String[] parts = placeName.split("\\s+");
        for (String part : parts) {
            if (part.length() > 2) { // Bỏ qua từ quá ngắn
                places = placeRepo.findByPlaceNameContainingIgnoreCase(part);
                if (!places.isEmpty()) {
                    return Optional.of(places.get(0));
                }
            }
        }

        // Fallback cuối cùng: tìm kiếm chính xác
        return placeRepo.findByPlaceName(placeName);
    }

    @Override
    public List<Place> findAllPlaces() {
        return placeRepo.findAll();
    }
}
