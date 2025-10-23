package com.group02.openevent.dto.form;

import com.group02.openevent.model.form.FormQuestion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {
    private String questionText;
    private FormQuestion.QuestionType questionType;
    private Boolean isRequired;
    private String questionOptions; // For SELECT, CHECKBOX, RADIO - line-separated options
    private Integer questionOrder;
    
    // Helper method to get options as a list
    public List<String> getQuestionOptionsAsList() {
        if (questionOptions == null || questionOptions.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(questionOptions.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
