package com.group02.openevent.dto.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventFormDTO {
    private Long formId;
    private Long eventId;
    private String formTitle;
    private String formDescription;
    private com.group02.openevent.model.form.EventForm.FormType formType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<FormQuestionDTO> questions;
}
