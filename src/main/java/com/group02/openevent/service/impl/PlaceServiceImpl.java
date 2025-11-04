package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import lombok.extern.slf4j.Slf4j;
import com.group02.openevent.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PlaceServiceImpl implements PlaceService {
    @Autowired
    IPlaceRepo placeRepo;

    @Override
    public List<Place> getAllByEventId(Long id) {


        // First, check if there are any places in the database at all
        List<Place> allPlaces = placeRepo.findAll();


        // Try JPQL query first
        List<Place> places = placeRepo.findPlacesByEventId(id);


        // If no results, try native query
        if (places.isEmpty()) {

            places = placeRepo.findPlacesByEventIdNative(id);

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

        // 1. Tìm kiếm linh hoạt với tên đã làm sạch (Lần 1)
        List<Place> places = placeRepo.findByPlaceNameContainingIgnoreCase(cleanedPlaceName);
        if (!places.isEmpty()) {
            return Optional.of(places.get(0));
        }

        // 2. TỐI ƯU HÓA: Chỉ tìm kiếm tên gốc NẾU nó khác tên đã làm sạch
        if (!cleanedPlaceName.equals(placeName)) {
            places = placeRepo.findByPlaceNameContainingIgnoreCase(placeName); // (Lần 2)
            if (!places.isEmpty()) {
                return Optional.of(places.get(0));
            }
        }

        // 3. Thử tìm kiếm từng phần của tên
        String[] parts = placeName.split("\\s+");
        for (String part : parts) {
            if (part.length() > 2) {
                places = placeRepo.findByPlaceNameContainingIgnoreCase(part); // (Lần 3 - hoặc 2 nếu Lần 2 bị bỏ qua)
                if (!places.isEmpty()) {
                    return Optional.of(places.get(0));
                }
            }
        }

        // 4. Fallback cuối cùng
        return placeRepo.findByPlaceName(placeName);
    }

    @Override
    public List<Place> findAllPlaces() {
        return placeRepo.findAll();
    }
}
