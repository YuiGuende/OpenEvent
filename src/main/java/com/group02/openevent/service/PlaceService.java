package com.group02.openevent.service;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Place;

import java.util.Optional;

public interface PlaceService {
    Optional<Place> findPlaceById(int id);
    Optional<Place> findPlaceByName(String placeName);
}
