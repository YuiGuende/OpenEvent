// File: com/group02/openevent/service/AiService.java
package com.group02.openevent.service;

import org.springframework.http.ResponseEntity;
// Bỏ import MultipartFile

public interface AiService {

    /**
     * Thay đổi: Chấp nhận raw JSON body (dưới dạng String) từ controller,
     * vì SDK đang gửi JSON, không phải file.
     */
    ResponseEntity<String> ocrId(String jsonBody);

    /**
     * Thay đổi: Chấp nhận raw JSON body.
     */
    ResponseEntity<String> cardLiveness(String jsonBody);

    /**
     * Thay đổi: Chấp nhận raw JSON body.
     */
    ResponseEntity<String> faceCompare(String jsonBody);

    /**
     * Thay đổi: Chấp nhận raw JSON body.
     */
    ResponseEntity<String> faceMask(String jsonBody);

    /**
     * Thay đổi: Chấp nhận raw JSON body.
     */
    ResponseEntity<String> liveness3d(String jsonBody);
}