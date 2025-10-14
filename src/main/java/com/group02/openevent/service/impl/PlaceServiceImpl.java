package com.group02.openevent.service.impl;


import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlaceServiceImpl implements PlaceService {

    @Autowired
    private IPlaceRepo placeRepo;

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