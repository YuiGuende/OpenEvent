package com.group02.openevent.repository;

import com.group02.openevent.model.event.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPlaceRepo extends JpaRepository<Place, Integer> {
    Optional<Place> findByPlaceName(String placeName);
    Optional<Place> findById(int id);
}
