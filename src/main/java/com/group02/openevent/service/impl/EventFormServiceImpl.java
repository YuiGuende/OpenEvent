package com.group02.openevent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.dto.form.*;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.form.EventForm;
import com.group02.openevent.model.form.FormQuestion;
import com.group02.openevent.model.form.FormResponse;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IEventFormRepo;
import com.group02.openevent.repository.IFormQuestionRepo;
import com.group02.openevent.repository.IFormResponseRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.EventFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventFormServiceImpl implements EventFormService {

    private final IEventFormRepo eventFormRepo;
    private final IFormQuestionRepo formQuestionRepo;
    private final IFormResponseRepo formResponseRepo;
    private final IEventRepo eventRepo;
    private final ICustomerRepo customerRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EventFormDTO createForm(CreateFormRequest request) {
        Event event = eventRepo.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Validate type
        if (request.getFormType() == null) {
            throw new RuntimeException("Form type is required (REGISTER/CHECKIN/FEEDBACK)");
        }
        
        // Upsert: if an active form of this type already exists for the event, update it instead of inserting a new one
        EventForm form = eventFormRepo.findActiveByEventIdAndType(request.getEventId(), request.getFormType())
                .orElseGet(EventForm::new);
        form.setEvent(event);
        form.setFormTitle(request.getFormTitle());
        form.setFormDescription(request.getFormDescription());
        form.setFormType(request.getFormType());
        form.setIsActive(true);

        EventForm savedForm = eventFormRepo.save(form);

        // Create questions
        if (request.getQuestions() != null) {
            for (CreateQuestionRequest questionRequest : request.getQuestions()) {
                FormQuestion question = new FormQuestion();
                question.setEventForm(savedForm);
                question.setQuestionText(questionRequest.getQuestionText());
                question.setQuestionType(questionRequest.getQuestionType());
                question.setIsRequired(questionRequest.getIsRequired() != null ? questionRequest.getIsRequired() : false);
                question.setQuestionOrder(questionRequest.getQuestionOrder());

                // Convert options to JSON
                List<String> optionsList = questionRequest.getQuestionOptionsAsList();
                if (optionsList != null && !optionsList.isEmpty()) {
                    try {
                        question.setQuestionOptions(objectMapper.writeValueAsString(optionsList));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error converting options to JSON", e);
                    }
                }

                formQuestionRepo.save(question);
            }
        }

        return convertToDTO(savedForm);
    }

    @Override
    public EventFormDTO getFormByEventId(Long eventId) {
        EventForm form = eventFormRepo.findByEventIdAndActive(eventId)
                .orElseThrow(() -> new RuntimeException("No active form found for event"));
        return convertToDTO(form);
    }

    @Override
    public EventFormDTO getActiveFormByEventIdAndType(Long eventId, EventForm.FormType formType) {
        EventForm form = eventFormRepo.findActiveByEventIdAndType(eventId, formType)
                .orElseThrow(() -> new RuntimeException("No active " + formType + " form found for event"));
        return convertToDTO(form);
    }

    @Override
    public EventFormDTO getFormById(Long formId) {
        EventForm form = eventFormRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));
        return convertToDTO(form);
    }

    @Override
    public List<EventFormDTO> getAllFormsByEventId(Long eventId) {
        List<EventForm> forms = eventFormRepo.findByEventId(eventId);
        return forms.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFormDTO updateForm(Long formId, CreateFormRequest request) {
        EventForm form = eventFormRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        form.setFormTitle(request.getFormTitle());
        form.setFormDescription(request.getFormDescription());

        // Delete existing questions
        formQuestionRepo.deleteByEventFormFormId(formId);

        // Create new questions
        if (request.getQuestions() != null) {
            for (CreateQuestionRequest questionRequest : request.getQuestions()) {
                FormQuestion question = new FormQuestion();
                question.setEventForm(form);
                question.setQuestionText(questionRequest.getQuestionText());
                question.setQuestionType(questionRequest.getQuestionType());
                question.setIsRequired(questionRequest.getIsRequired() != null ? questionRequest.getIsRequired() : false);
                question.setQuestionOrder(questionRequest.getQuestionOrder());

                List<String> optionsList = questionRequest.getQuestionOptionsAsList();
                if (optionsList != null && !optionsList.isEmpty()) {
                    try {
                        question.setQuestionOptions(objectMapper.writeValueAsString(optionsList));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error converting options to JSON", e);
                    }
                }

                formQuestionRepo.save(question);
            }
        }

        EventForm savedForm = eventFormRepo.save(form);
        return convertToDTO(savedForm);
    }

    @Override
    @Transactional
    public void deleteForm(Long formId) {
        eventFormRepo.deleteById(formId);
    }

    @Override
    public List<FormQuestionDTO> getQuestionsByFormId(Long formId) {
        List<FormQuestion> questions = formQuestionRepo.findByFormIdOrderByOrder(formId);
        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FormQuestionDTO addQuestionToForm(Long formId, FormQuestionDTO questionDTO) {
        EventForm form = eventFormRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        FormQuestion question = new FormQuestion();
        question.setEventForm(form);
        question.setQuestionText(questionDTO.getQuestionText());
        question.setQuestionType(questionDTO.getQuestionType());
        question.setIsRequired(questionDTO.getIsRequired());
        question.setQuestionOptions(questionDTO.getQuestionOptions());
        question.setQuestionOrder(questionDTO.getQuestionOrder());

        FormQuestion savedQuestion = formQuestionRepo.save(question);
        return convertQuestionToDTO(savedQuestion);
    }

    @Override
    public FormQuestionDTO updateQuestion(Long questionId, FormQuestionDTO questionDTO) {
        FormQuestion question = formQuestionRepo.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        question.setQuestionText(questionDTO.getQuestionText());
        question.setQuestionType(questionDTO.getQuestionType());
        question.setIsRequired(questionDTO.getIsRequired());
        question.setQuestionOptions(questionDTO.getQuestionOptions());
        question.setQuestionOrder(questionDTO.getQuestionOrder());

        FormQuestion savedQuestion = formQuestionRepo.save(question);
        return convertQuestionToDTO(savedQuestion);
    }

    @Override
    public void deleteQuestion(Long questionId) {
        formQuestionRepo.deleteById(questionId);
    }

    @Override
    @Transactional
    public void submitResponse(SubmitResponseRequest request) {
        EventForm form = eventFormRepo.findById(request.getFormId())
                .orElseThrow(() -> new RuntimeException("Form not found"));

        // Use getReferenceById to get a proxy without loading all relationships
        // This avoids duplicate row issues when Customer has OneToOne with Host
        // Note: getReferenceById returns a proxy and will throw EntityNotFoundException
        // if the entity doesn't exist when first accessed
        Customer customer = customerRepo.getReferenceById(request.getCustomerId());

        for (SubmitResponseRequest.ResponseItem item : request.getResponses()) {
            if (item == null) {
                System.err.println("Warning: Null response item skipped");
                continue;
            }
            
            if (item.getQuestionId() == null) {
                System.err.println("Warning: Response item with null questionId skipped");
                continue;
            }
            
            FormQuestion question = formQuestionRepo.findById(item.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question not found: " + item.getQuestionId()));

            FormResponse response = new FormResponse();
            response.setEventForm(form);
            response.setCustomer(customer);
            response.setFormQuestion(question);
            response.setResponseValue(item.getResponseValue() != null ? item.getResponseValue() : "");

            formResponseRepo.save(response);
            System.out.println("Saved response for question " + item.getQuestionId() + " with value: " + item.getResponseValue());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormResponseDTO> getResponsesByFormId(Long formId) {
        List<FormResponse> responses = formResponseRepo.findByFormId(formId);
        return responses.stream()
                .map(this::convertResponseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormResponseDTO> getResponsesByEventId(Long eventId) {
        List<FormResponse> responses = formResponseRepo.findByEventId(eventId);
        return responses.stream()
                .sorted((r1, r2) -> r2.getSubmittedAt().compareTo(r1.getSubmittedAt())) // Sort by newest first
                .map(this::convertResponseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormResponseDTO> getUserResponsesByFormId(Long formId, Long customerId) {
        List<FormResponse> responses = formResponseRepo.findByCustomerIdAndFormId(customerId, formId);
        return responses.stream()
                .map(this::convertResponseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormResponseDTO> getResponsesByEventIdAndFormType(Long eventId, com.group02.openevent.model.form.EventForm.FormType formType) {
        List<FormResponse> responses = formResponseRepo.findByEventIdAndFormType(eventId, formType);
        return responses.stream()
                .sorted((r1, r2) -> r2.getSubmittedAt().compareTo(r1.getSubmittedAt())) // Sort by newest first
                .map(this::convertResponseToDTO)
                .collect(Collectors.toList());
    }

    private EventFormDTO convertToDTO(EventForm form) {
        EventFormDTO dto = new EventFormDTO();
        dto.setFormId(form.getFormId());
        dto.setEventId(form.getEvent().getId());
        dto.setFormTitle(form.getFormTitle());
        dto.setFormDescription(form.getFormDescription());
        dto.setFormType(form.getFormType());
        dto.setIsActive(form.getIsActive());
        dto.setCreatedAt(form.getCreatedAt());

        // Sửa: Dùng query có sắp xếp thay vì lazy loading để tránh duplicate và đảm bảo thứ tự
        List<FormQuestion> questions = formQuestionRepo.findByFormIdOrderByOrder(form.getFormId());
        if (questions != null && !questions.isEmpty()) {
            dto.setQuestions(questions.stream()
                    .map(this::convertQuestionToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private FormQuestionDTO convertQuestionToDTO(FormQuestion question) {
        FormQuestionDTO dto = new FormQuestionDTO();
        dto.setQuestionId(question.getQuestionId());
        dto.setFormId(question.getEventForm().getFormId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setIsRequired(question.getIsRequired());
        dto.setQuestionOptions(question.getQuestionOptions());
        dto.setQuestionOrder(question.getQuestionOrder());
        dto.setCreatedAt(question.getCreatedAt());
        return dto;
    }

    private FormResponseDTO convertResponseToDTO(FormResponse response) {
        FormResponseDTO dto = new FormResponseDTO();
        dto.setResponseId(response.getResponseId());
        dto.setFormId(response.getEventForm().getFormId());
        
        // Customer might be a lazy proxy - access it within transaction
        if (response.getCustomer() != null) {
            dto.setCustomerId(response.getCustomer().getCustomerId());
            dto.setCustomerName(response.getCustomer().getName() != null ? response.getCustomer().getName() : "");
            dto.setCustomerEmail(response.getCustomer().getEmail() != null ? response.getCustomer().getEmail() : "");
        }
        
        // Form type
        dto.setFormType(response.getEventForm().getFormType());
        
        // Question information
        if (response.getFormQuestion() != null) {
            dto.setQuestionId(response.getFormQuestion().getQuestionId());
            dto.setQuestionText(response.getFormQuestion().getQuestionText() != null ? response.getFormQuestion().getQuestionText() : "");
        }
        
        dto.setResponseValue(response.getResponseValue());
        dto.setSubmittedAt(response.getSubmittedAt());
        return dto;
    }
}
