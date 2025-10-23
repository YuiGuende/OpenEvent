package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.IPlaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
