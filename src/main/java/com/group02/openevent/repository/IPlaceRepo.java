package com.group02.openevent.repository;

import com.group02.openevent.model.event.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPlaceRepo extends JpaRepository<Place, Long> {
    Optional<Place> findByPlaceName(String placeName);
    Optional<Place> findById(Long placeId);
    List<Place> findAll();
    
    // Tìm kiếm linh hoạt theo tên phòng và tòa nhà
    @Query(value = "SELECT * FROM place WHERE " +
           "LOWER(place_name) LIKE LOWER(CONCAT('%', :placeName, '%')) " +
           "OR LOWER(building) LIKE LOWER(CONCAT('%', :placeName, '%'))", 
           nativeQuery = true)
    List<Place> findByPlaceNameContainingIgnoreCase(@Param("placeName") String placeName);
}
