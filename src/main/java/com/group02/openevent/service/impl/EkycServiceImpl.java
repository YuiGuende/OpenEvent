package com.group02.openevent.service.impl;

import com.group02.openevent.service.EkycService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EkycServiceImpl implements EkycService {

    @Value("${vnpt.api.url}")
    private String vnptApiUrl;

    @Value("${vnpt.token.id}")
    private String tokenId;

    @Value("${vnpt.token.key}")
    private String tokenKey;

    @Value("${vnpt.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate;

    public EkycServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ResponseEntity<String> uploadFile(MultipartFile file) {
        try {
            String url = vnptApiUrl + "/addFile";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Token-Id", tokenId);
            headers.set("Token-Key", tokenKey);
            headers.set("Authorization", "Bearer " + accessToken);
//            headers.set("Mac-Address", "WEB-001");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            // VNPT yêu cầu metadata: title, description, type... (tùy doc)
            body.add("title", "ID_CARD_FRONT");
            body.add("description", "Ảnh mặt trước CCCD");
//            body.add("type", "4");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
