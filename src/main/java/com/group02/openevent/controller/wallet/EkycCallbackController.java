// File: com/group02/openevent/controller/wallet/EkycCallbackController.java
package com.group02.openevent.controller.wallet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.model.user.User;
import com.group02.openevent.service.IHostWalletService;
import com.group02.openevent.service.CustomerService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ekyc")
public class EkycCallbackController {

    private final ObjectMapper objectMapper;
    private final IHostWalletService hostWalletService;
    private final CustomerService customerService;
    private final UserService userService;

    public EkycCallbackController(ObjectMapper objectMapper,
                                  IHostWalletService hostWalletService,
                                  CustomerService customerService, UserService userService) {
        this.objectMapper = objectMapper;
        this.hostWalletService = hostWalletService;
        this.customerService = customerService;
        this.userService = userService;
    }

    /**
     * Endpoint này nhận KẾT QUẢ CUỐI CÙNG từ SDK (CALL_BACK_END_FLOW).
     * Đây là nơi chính xác để lấy tên và lưu vào session/DB.
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(@RequestBody Map<String, Object> payload,
                                                              HttpSession session) {
        System.out.println("--- CALLBACK: /api/ekyc/callback (Kết quả cuối cùng) ---");

        try {
            // Cấu trúc payload dự kiến: { ..., "ocr": { "object": { "name": "...", ... } }, ... }

            // 1. Lấy object 'ocr' từ payload
            Object ocrObject = payload.get("ocr");

            if (ocrObject != null) {
                // 2. Chuyển đổi 'ocrObject' thành Map
                Map<String, Object> ocrMap = objectMapper.convertValue(
                        ocrObject,
                        new TypeReference<Map<String, Object>>() {
                        }
                );

                // 3. Lấy 'object' từ 'ocrMap' (theo lời bạn: "object nằm trong ocr")
                Object dataObject = ocrMap.get("object");

                if (dataObject != null) {
                    // 4. Chuyển đổi 'dataObject' thành Map
                    Map<String, Object> objectMap = objectMapper.convertValue(
                            dataObject,
                            new TypeReference<Map<String, Object>>() {
                            }
                    );

                    // 5. Lấy 'name' từ 'objectMap' (theo lời bạn: "name nằm trong object")
                    String name = (String) objectMap.get("name");

                    if (name != null && !name.trim().isEmpty()) {
                        System.out.println("===> TÊN KHÁCH HÀNG (từ ocr.object): " + name);

                        // 6. Lưu KYC name vào session để dùng khi tạo ví
                        session.setAttribute("KYC_NAME", name.trim());
                        System.out.println("===> Đã lưu KYC name vào session: " + name.trim());

                        // 7. Nếu đã có ví, cập nhật KYC name vào ví
                        try {
                            User user = userService.getCurrentUser(session);
                            if (user != null && user.getHost() != null) {
                                Long hostId = user.getHost().getId();
                                // Cập nhật KYC name vào ví nếu đã có ví
                                hostWalletService.updateKycName(hostId, name.trim());
                                System.out.println("===> Đã cập nhật KYC name vào ví của host: " + hostId);
                            }
                        } catch (Exception e) {
                            System.out.println("===> Chưa có ví, sẽ lưu vào session để dùng sau: " + e.getMessage());
                        }

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "KYC verification successful");
                        response.put("name", name.trim());
                        return ResponseEntity.ok(response);
                    } else {
                        System.out.println("===> Không tìm thấy 'name' trong 'ocr.object'.");
                    }
                } else {
                    System.out.println("===> Không tìm thấy 'object' trong 'ocr'.");
                }
            } else {
                System.out.println("===> Không tìm thấy 'ocr' object trong payload callback.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Không tìm thấy thông tin KYC trong callback");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            System.err.println("!!! Lỗi khi xử lý callback eKYC: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xử lý callback eKYC: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}