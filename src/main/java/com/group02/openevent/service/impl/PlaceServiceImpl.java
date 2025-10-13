package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.IPlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaceServiceImpl implements IPlaceService {
    @Autowired
    IPlaceRepo placeRepo;

    @Override
    public List<Place> getAllByEventId(Long id) {
        return placeRepo.findPlacesByEventId(id);
    }
}
