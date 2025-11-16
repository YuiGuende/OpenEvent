package com.group02.openevent.repository;

import com.group02.openevent.model.ticket.TicketType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ITicketTypeRepo extends JpaRepository<TicketType, Long> {
    
    /**
     * Find ticket type with pessimistic write lock to prevent race conditions
     * when reserving tickets
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.ticketTypeId = :id")
    Optional<TicketType> findByIdForUpdate(@Param("id") Long id);

    List<TicketType> findByEventId(Long eventId);

    @Query("SELECT tt FROM TicketType tt WHERE tt.event.id = :eventId " +
            "AND tt.totalQuantity > tt.soldQuantity " +
            "AND (tt.startSaleDate IS NULL OR tt.startSaleDate <= :now) " +
            "AND (tt.endSaleDate IS NULL OR tt.endSaleDate >= :now)")
    List<TicketType> findAvailableByEventId(@Param("eventId") Long eventId, @Param("now") LocalDateTime now);

    @Query("SELECT tt FROM TicketType tt WHERE tt.event.id = :eventId " +
            "AND tt.totalQuantity > tt.soldQuantity")
    List<TicketType> findAvailableByEventIdIgnoreTime(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(tt) > 0 FROM TicketType tt WHERE tt.ticketTypeId = :id " +
            "AND tt.totalQuantity > tt.soldQuantity " +
            "AND (tt.startSaleDate IS NULL OR tt.startSaleDate <= :now) " +
            "AND (tt.endSaleDate IS NULL OR tt.endSaleDate >= :now)")
    boolean isAvailableNow(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Query("SELECT tt FROM TicketType tt WHERE tt.price BETWEEN :minPrice AND :maxPrice")
    List<TicketType> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                      @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT SUM(tt.soldQuantity) FROM TicketType tt WHERE tt.event.id = :eventId")
    Integer getTotalSoldByEventId(@Param("eventId") Long eventId);

    @Query("SELECT SUM(tt.totalQuantity - tt.soldQuantity) FROM TicketType tt WHERE tt.event.id = :eventId")
    Integer getTotalAvailableByEventId(@Param("eventId") Long eventId);

    @Query("SELECT SUM(t.totalQuantity) FROM TicketType t WHERE t.event.id = :eventId")
    Integer getTotalTicketCapacityByEventId(@Param("eventId") Long eventId);
}