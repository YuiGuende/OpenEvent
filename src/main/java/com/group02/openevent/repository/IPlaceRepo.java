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

    @Query("SELECT p FROM Place p JOIN p.events e WHERE e.id = :eventId")
    List<Place> findPlacesByEventId(@Param("eventId") Long eventId);

    // Tìm kiếm linh hoạt theo tên phòng và tòa nhà
    @Query(value = "SELECT * FROM place WHERE " +
            "LOWER(place_name) LIKE LOWER(CONCAT('%', :placeName, '%')) " +
            "OR LOWER(building) LIKE LOWER(CONCAT('%', :placeName, '%'))",
            nativeQuery = true)
    List<Place> findByPlaceNameContainingIgnoreCase(@Param("placeName") String placeName);

    @Query(value = "SELECT p.* FROM place p INNER JOIN event_place ep ON p.place_id = ep.place_id WHERE ep.event_id = :eventId", nativeQuery = true)
    List<Place> findPlacesByEventIdNative(@Param("eventId") Long eventId);
}
