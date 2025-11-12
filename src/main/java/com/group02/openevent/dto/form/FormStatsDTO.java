package com.group02.openevent.dto.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for form statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormStatsDTO {
    private Long formId;
    private String formTitle;
    private String formType;
    private Long totalResponses; // Total number of responses
    private Long totalSubmissions; // Total number of unique customers who submitted
    private List<QuestionStatsDTO> questionStats; // Statistics for each question
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionStatsDTO {
        private Long questionId;
        private String questionText;
        private String questionType; // TEXT, RADIO, CHECKBOX, SELECT, etc.
        private Long responseCount; // Number of responses for this question
        private List<OptionCountDTO> optionCounts; // For RADIO, CHECKBOX, SELECT: count per option
        private List<String> textResponses; // For TEXT, TEXTAREA: sample responses (first 10)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionCountDTO {
        private String option;
        private Long count;
    }
}

