package com.group02.openevent.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface EkycService {
    ResponseEntity<String> uploadFile(MultipartFile file);
}