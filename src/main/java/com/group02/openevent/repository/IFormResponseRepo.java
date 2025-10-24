package com.group02.openevent.repository;

import com.group02.openevent.model.form.FormResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IFormResponseRepo extends JpaRepository<FormResponse, Long> {
    
    @Query("SELECT r FROM FormResponse r WHERE r.eventForm.formId = :formId")
    List<FormResponse> findByFormId(@Param("formId") Long formId);
    
    @Query("SELECT r FROM FormResponse r WHERE r.customer.customerId = :customerId AND r.eventForm.formId = :formId")
    List<FormResponse> findByCustomerIdAndFormId(@Param("customerId") Long customerId, @Param("formId") Long formId);
    
    @Query("SELECT r FROM FormResponse r WHERE r.eventForm.event.id = :eventId")
    List<FormResponse> findByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT r FROM FormResponse r WHERE r.customer.customerId = :customerId AND r.eventForm.event.id = :eventId")
    List<FormResponse> findByCustomerIdAndEventId(@Param("customerId") Long customerId, @Param("eventId") Long eventId);
}
