package com.group02.openevent.service.impl;


import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlaceServiceImpl implements PlaceService {

    @Autowired
    private IPlaceRepo placeRepo;


    @Override
    public Optional<Place> findPlaceById(int id) {
        return placeRepo.findById(id);
    }

    @Override
    public Optional<Place> findPlaceByName(String placeName) {
        return placeRepo.findByPlaceName(placeName);
    }
}