package com.group02.openevent.repository;

import com.group02.openevent.model.form.FormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IFormQuestionRepo extends JpaRepository<FormQuestion, Long> {
    
    @Query("SELECT q FROM FormQuestion q WHERE q.eventForm.formId = :formId ORDER BY q.questionOrder ASC")
    List<FormQuestion> findByFormIdOrderByOrder(@Param("formId") Long formId);
    
    @Query("SELECT q FROM FormQuestion q WHERE q.eventForm.event.id = :eventId ORDER BY q.questionOrder ASC")
    List<FormQuestion> findByEventIdOrderByOrder(@Param("eventId") Long eventId);
    
    void deleteByEventFormFormId(Long formId);
}
