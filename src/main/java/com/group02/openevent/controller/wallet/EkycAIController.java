// File: com/group02/openevent/controller/wallet/EkycAIController.java
package com.group02.openevent.controller.wallet;

import com.group02.openevent.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Bỏ import MultipartFile

@RestController
@RequestMapping("/api/ekyc/ai/v1/web")
public class EkycAIController {

    private final AiService aiService;

    public EkycAIController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * SỬA LỖI:
     * Lỗi "Current request is not a multipart request" cho thấy SDK
     * đang gửi JSON (chứa hash), không phải là file.
     * Chúng ta thay @RequestParam MultipartFile bằng @RequestBody String
     * để nhận raw JSON.
     */
    @PostMapping("/ocr/id")
    public ResponseEntity<String> ocrId(@RequestBody String jsonBody) {
        System.out.println("--- EkycAIController: /ocr/id ---");
        System.out.println("Request Body: " + jsonBody);
        return aiService.ocrId(jsonBody);
    }

    @PostMapping("/card/liveness")
    public ResponseEntity<String> cardLiveness(@RequestBody String jsonBody) {
        System.out.println("--- EkycAIController: /card/liveness ---");
        System.out.println("Request Body: " + jsonBody);
        return aiService.cardLiveness(jsonBody);
    }

    @PostMapping("/face/compare")
    public ResponseEntity<String> faceCompare(@RequestBody String jsonBody) {
        System.out.println("--- EkycAIController: /face/compare ---");
        System.out.println("Request Body: " + jsonBody);
        return aiService.faceCompare(jsonBody);
    }

    @PostMapping("/face/mask")
    public ResponseEntity<String> faceMask(@RequestBody String jsonBody) {
        System.out.println("--- EkycAIController: /face/mask ---");
        System.out.println("Request Body: " + jsonBody);
        return aiService.faceMask(jsonBody);
    }

    @PostMapping("/face/liveness-3d")
    public ResponseEntity<String> liveness3d(@RequestBody String jsonBody) {
        System.out.println("--- EkycAIController: /face/liveness-3d ---");
        System.out.println("Request Body: " + jsonBody);
        return aiService.liveness3d(jsonBody);
    }
}