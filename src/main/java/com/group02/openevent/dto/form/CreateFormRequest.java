package com.group02.openevent.dto.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFormRequest {
    private Long eventId;
    private String formTitle;
    private String formDescription;
    private List<CreateQuestionRequest> questions;
}
