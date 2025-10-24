package com.group02.openevent.dto.form;

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
    private Long questionId;
    private String responseValue;
    private LocalDateTime submittedAt;
}
