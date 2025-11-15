package com.group02.openevent.service;

import com.group02.openevent.dto.form.*;

import java.util.List;

public interface EventFormService {
    
    // Form Management
    EventFormDTO createForm(CreateFormRequest request);
    EventFormDTO getFormByEventId(Long eventId);
    EventFormDTO getActiveFormByEventIdAndType(Long eventId, com.group02.openevent.model.form.EventForm.FormType formType);
    EventFormDTO getFormById(Long formId);
    List<EventFormDTO> getAllFormsByEventId(Long eventId);
    EventFormDTO updateForm(Long formId, CreateFormRequest request);
    void deleteForm(Long formId);
    
    // Question Management
    FormQuestionDTO addQuestionToForm(Long formId, FormQuestionDTO questionDTO);
    FormQuestionDTO updateQuestion(Long questionId, FormQuestionDTO questionDTO);
    void deleteQuestion(Long questionId);
    List<FormQuestionDTO> getQuestionsByFormId(Long formId);
    
    // Response Management
    void submitResponse(SubmitResponseRequest request);
    List<FormResponseDTO> getResponsesByFormId(Long formId);
    List<FormResponseDTO> getResponsesByEventId(Long eventId);
    List<FormResponseDTO> getUserResponsesByFormId(Long formId, Long customerId);
    List<FormResponseDTO> getResponsesByEventIdAndFormType(Long eventId, com.group02.openevent.model.form.EventForm.FormType formType);
    
    // Statistics
    FormStatsDTO getFormStatistics(Long formId);
}
