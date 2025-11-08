// File: com/group02/openevent/service/impl/AiServiceImpl.java
package com.group02.openevent.service.impl;

import com.group02.openevent.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
// Bỏ các import không cần thiết (MultipartFile, ObjectMapper, Map, v.v.)

@Service
public class AiKycServiceImpl implements AiService {

    @Value("${vnpt.api.ai.url}")
    private String vnptApiUrl; // Ví dụ: https://api.idg.vnpt.vn

    @Value("${vnpt.token.id}")
    private String tokenId;

    @Value("${vnpt.token.key}")
    private String tokenKey;

    @Value("${vnpt.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate;

    public AiKycServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * SỬA LỖI:
     * Logic 2 bước đã bị xóa.
     * Phương thức này giờ chỉ forward (chuyển tiếp) request JSON
     * nhận được từ SDK (thông qua Controller) đến VNPT API.
     *
     * @param endpoint    Đường dẫn API của VNPT (ví dụ: /ai/v1/ocr/id)
     * @param jsonBody    Nội dung JSON (dạng String) nhận từ SDK
     */
    private ResponseEntity<String> forwardJsonRequest(String endpoint, String jsonBody) {
        // Đường dẫn đầy đủ đến API của VNPT
        String url = vnptApiUrl + endpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token-id", tokenId);
        headers.set("Token-key", tokenKey);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("mac-address", "TEST1"); // Giữ nguyên theo tài liệu

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        System.out.println("--- AiServiceImpl: Forwarding to " + url + " ---");
        System.out.println("Forwarding Headers (một phần): " + headers.getFirst("Token-id")); // Che bớt log
        System.out.println("Forwarding Body: " + jsonBody);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            System.out.println("VNPT API Response Status: " + response.getStatusCode());
            // System.out.println("VNPT API Response Body: " + response.getBody()); // Tắt log này cho đỡ rối
            return response;

        } catch (HttpClientErrorException e) {
            System.err.println("!!! VNPT API Error: " + e.getStatusCode());
            System.err.println("!!! VNPT API Response: " + e.getResponseBodyAsString());
            // Trả về lỗi chi tiết từ VNPT
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("!!! Internal Error: " + e.getMessage());
            e.printStackTrace(); // In chi tiết lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    // --- Triển khai các API Service (đã sửa) ---

    @Override
    public ResponseEntity<String> ocrId(String jsonBody) {
        // SDK đang gọi /api/ekyc/ai/v1/web/ocr/id
        // Chúng ta forward nó đến /ai/v1/ocr/id (theo tài liệu API số 6)
        // **LƯU Ý QUAN TRỌNG:** Tên endpoint của SDK (web-sdk-version-3.2.0.0.js)
        // có thể yêu cầu endpoint /ai/v1/web/ocr/id.
        // Tôi sẽ dùng endpoint /ai/v1/ocr/id từ tài liệu API.
        // Nếu nó vẫn lỗi 400, HÃY THỬ THAY THÀNH:
        // return forwardJsonRequest("/ai/v1/web/ocr/id", jsonBody);
        return forwardJsonRequest("/ai/v1/ocr/id", jsonBody);
    }

    @Override
    public ResponseEntity<String> cardLiveness(String jsonBody) {
        // Forward đến API số 3
        return forwardJsonRequest("/ai/v1/card/liveness", jsonBody);
    }

    @Override
    public ResponseEntity<String> faceCompare(String jsonBody) {
        // Forward đến API số 7
        return forwardJsonRequest("/ai/v1/face/compare", jsonBody);
    }

    @Override
    public ResponseEntity<String> faceMask(String jsonBody) {
        // Forward đến API số 9
        return forwardJsonRequest("/ai/v1/face/mask", jsonBody);
    }

    @Override
    public ResponseEntity<String> liveness3d(String jsonBody) {
        // SDK gọi "liveness-3d", nhưng tài liệu API là "liveness" (API số 8)
        // Tôi sẽ dùng /ai/v1/face/liveness
        return forwardJsonRequest("/ai/v1/face/liveness", jsonBody);
    }
}