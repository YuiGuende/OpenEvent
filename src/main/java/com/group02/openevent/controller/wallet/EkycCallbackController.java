package com.group02.openevent.controller.wallet;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ekyc")
public class EkycCallbackController {

    @PostMapping("/callback")
    public void handleCallback(@RequestBody Map<String, Object> payload) {
        // Controller nhận kết quả SDK gửi từ frontend (CALL_BACK_END_FLOW)
        System.out.println("Received eKYC callback: " );
        // TODO: lưu db hoặc push xử lý tiếp
    }
}