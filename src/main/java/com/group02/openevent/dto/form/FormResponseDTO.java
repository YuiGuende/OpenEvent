package com.group02.openevent.dto.form;

import com.group02.openevent.model.form.EventForm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormResponseDTO {
    private Long responseId;
    private Long formId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long questionId;
    private String questionText;
    private String responseValue;
    private LocalDateTime submittedAt;
    private EventForm.FormType formType;
}
