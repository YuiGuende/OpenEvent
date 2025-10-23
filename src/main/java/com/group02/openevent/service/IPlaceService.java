package com.group02.openevent.service;

import com.group02.openevent.model.event.Place;

import java.util.List;

public interface IPlaceService {
    public List<Place> getAllByEventId(Long id);
}
