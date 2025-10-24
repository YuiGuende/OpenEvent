package com.group02.openevent.dto.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.model.form.FormQuestion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormQuestionDTO {
    private Long questionId;
    private Long formId;
    private String questionText;
    private FormQuestion.QuestionType questionType;
    private Boolean isRequired;
    private String questionOptions; // JSON string
    private Integer questionOrder;
    private LocalDateTime createdAt;
    
    // Helper method to parse JSON options for Thymeleaf
    public List<String> getOptionsList() {
        if (questionOptions == null || questionOptions.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(questionOptions, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // Fallback: treat as comma-separated string
            return List.of(questionOptions.split(","));
        }
    }
}
