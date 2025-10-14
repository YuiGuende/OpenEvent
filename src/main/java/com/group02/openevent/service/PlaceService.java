package com.group02.openevent.service;

import com.group02.openevent.model.event.Place;

import java.util.List;
import java.util.Optional;

public interface PlaceService {
    Optional<Place> findPlaceById(Long placeId);
    Optional<Place> findPlaceByName(String placeName);
    List<Place> findAllPlaces();
    Optional<Place> findPlaceByNameFlexible(String placeName);
}
