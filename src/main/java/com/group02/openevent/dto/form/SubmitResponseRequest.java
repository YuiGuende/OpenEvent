package com.group02.openevent.dto.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResponseRequest {
    private Long formId;
    private Long customerId;
    private List<ResponseItem> responses;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseItem {
        private Long questionId;
        private String responseValue;
    }
}
