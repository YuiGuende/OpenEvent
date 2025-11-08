package com.group02.openevent.controller.wallet;

import com.group02.openevent.service.EkycService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ekyc/file-service/v1")
public class EkycController {

    private final EkycService ekycService;

    public EkycController(EkycService ekycService) {
        this.ekycService = ekycService;
    }

    // SDK gửi file -> backend -> service forward to VNPT
    @PostMapping("/addFile")
    public ResponseEntity<String> addFile(@RequestParam("file") MultipartFile file) {
        // controller không xử lý challengeCode (SDK có thể gửi), service tự thêm metadata
        return ekycService.uploadFile(file);
    }
}
