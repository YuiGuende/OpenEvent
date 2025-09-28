package com.group02.openevent.controller;

import com.group02.openevent.dto.payment.PayOSPaymentRequest;
import com.group02.openevent.dto.payment.PayOSPaymentResponse;
import com.group02.openevent.dto.payment.PayOSItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class PayOSDebugController {

    private final RestTemplate restTemplate;

    @Value("${payos.client.id:}")
    private String payosClientId;

    @Value("${payos.api.key:}")
    private String payosApiKey;

    @Value("${payos.checksum.key:}")
    private String payosChecksumKey;

    @Value("${payos.base.url:https://api-merchant.payos.vn}")
    private String payosBaseUrl;

    public PayOSDebugController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Test PayOS API connection and credentials
     */
    @GetMapping("/payos/test")
    public ResponseEntity<Map<String, Object>> testPayOSConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test credentials
            result.put("clientId", payosClientId);
            result.put("apiKey", payosApiKey != null ? "***" + payosApiKey.substring(Math.max(0, payosApiKey.length() - 4)) : "null");
            result.put("checksumKey", payosChecksumKey != null ? "***" + payosChecksumKey.substring(Math.max(0, payosChecksumKey.length() - 4)) : "null");
            result.put("baseUrl", payosBaseUrl);
            
            // Test API endpoint
            String testUrl = payosBaseUrl + "/v2/payment-requests";
            result.put("testUrl", testUrl);
            
            // Create a simple test request
            PayOSPaymentRequest testRequest = new PayOSPaymentRequest();
            testRequest.setOrderCode(999999);
            testRequest.setAmount(1000);
            testRequest.setDescription("Test Payment");
            testRequest.setItems(Arrays.asList(
                new PayOSItem("Test Item", 1, 1000)
            ));
            testRequest.setReturnUrl("http://localhost:8080/payment/success");
            testRequest.setCancelUrl("http://localhost:8080/payment/cancel");
            testRequest.setExpiredAt(System.currentTimeMillis() / 1000 + 900);
            testRequest.setSignature("test_signature");
            
            result.put("testRequest", testRequest);
            
            // Try to make API call
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payosClientId);
            headers.set("x-api-key", payosApiKey);
            
            HttpEntity<PayOSPaymentRequest> entity = new HttpEntity<>(testRequest, headers);
            
            try {
                ResponseEntity<PayOSPaymentResponse> response = restTemplate.postForEntity(
                    testUrl, entity, PayOSPaymentResponse.class
                );
                
                result.put("apiCallSuccess", true);
                result.put("responseStatus", response.getStatusCode());
                result.put("responseBody", response.getBody());
                result.put("responseCode", response.getBody() != null ? response.getBody().getCode() : "null");
                result.put("responseDesc", response.getBody() != null ? response.getBody().getDesc() : "null");
                result.put("responseData", response.getBody() != null ? response.getBody().getData() : "null");
                
            } catch (Exception e) {
                result.put("apiCallSuccess", false);
                result.put("apiError", e.getMessage());
                result.put("apiErrorClass", e.getClass().getSimpleName());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Test different signature formats
     */
    @GetMapping("/payos/test-signatures")
    public ResponseEntity<Map<String, Object>> testSignatures() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test different signature formats
            int orderCode = 123456;
            int amount = 250000;
            String description = "Event Registration";
            
            // Format 1: orderCode|amount|description
            String dataStr1 = orderCode + "|" + amount + "|" + description;
            String signature1 = generateSignature(dataStr1);
            
            // Format 2: orderCode|amount|description|returnUrl|cancelUrl
            String returnUrl = "http://localhost:8080/payment/success";
            String cancelUrl = "http://localhost:8080/payment/cancel";
            String dataStr2 = orderCode + "|" + amount + "|" + description + "|" + returnUrl + "|" + cancelUrl;
            String signature2 = generateSignature(dataStr2);
            
            // Format 3: amount|orderCode|description
            String dataStr3 = amount + "|" + orderCode + "|" + description;
            String signature3 = generateSignature(dataStr3);
            
            result.put("orderCode", orderCode);
            result.put("amount", amount);
            result.put("description", description);
            result.put("format1_data", dataStr1);
            result.put("format1_signature", signature1);
            result.put("format2_data", dataStr2);
            result.put("format2_signature", signature2);
            result.put("format3_data", dataStr3);
            result.put("format3_signature", signature3);
            result.put("checksumKeyLength", payosChecksumKey != null ? payosChecksumKey.length() : "null");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    private String generateSignature(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                payosChecksumKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Test PayOS API with minimal request
     */
    @GetMapping("/payos/test-minimal")
    public ResponseEntity<Map<String, Object>> testMinimalPayOSRequest() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Create minimal request
            Map<String, Object> minimalRequest = new HashMap<>();
            minimalRequest.put("orderCode", 123456);
            minimalRequest.put("amount", 1000);
            minimalRequest.put("description", "Test Payment");
            minimalRequest.put("returnUrl", "http://localhost:8080/payment/success");
            minimalRequest.put("cancelUrl", "http://localhost:8080/payment/cancel");
            minimalRequest.put("expiredAt", System.currentTimeMillis() / 1000 + 900); // 15 minutes
            
            result.put("request", minimalRequest);
            
            // Try API call
            String testUrl = payosBaseUrl + "/v2/payment-requests";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payosClientId);
            headers.set("x-api-key", payosApiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(minimalRequest, headers);
            
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(testUrl, entity, String.class);
                
                result.put("success", true);
                result.put("status", response.getStatusCode());
                result.put("response", response.getBody());
                
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
                result.put("errorClass", e.getClass().getSimpleName());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Test PayOS API with different request format
     */
    @PostMapping("/payos/test-request")
    public ResponseEntity<Map<String, Object>> testPayOSRequest(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Create request manually to test different formats
            Map<String, Object> manualRequest = new HashMap<>();
            manualRequest.put("orderCode", requestData.getOrDefault("orderCode", 123456));
            manualRequest.put("amount", requestData.getOrDefault("amount", 1000));
            manualRequest.put("description", requestData.getOrDefault("description", "Test Payment"));
            manualRequest.put("returnUrl", requestData.getOrDefault("returnUrl", "http://localhost:8080/payment/success"));
            manualRequest.put("cancelUrl", requestData.getOrDefault("cancelUrl", "http://localhost:8080/payment/cancel"));
            manualRequest.put("expiredAt", System.currentTimeMillis() / 1000 + 900);
            manualRequest.put("signature", "test_signature");
            
            result.put("manualRequest", manualRequest);
            
            // Try API call
            String testUrl = payosBaseUrl + "/v2/payment-requests";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payosClientId);
            headers.set("x-api-key", payosApiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(manualRequest, headers);
            
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(
                    testUrl, entity, String.class
                );
                
                result.put("apiCallSuccess", true);
                result.put("responseStatus", response.getStatusCode());
                result.put("responseBody", response.getBody());
                
            } catch (Exception e) {
                result.put("apiCallSuccess", false);
                result.put("apiError", e.getMessage());
                result.put("apiErrorClass", e.getClass().getSimpleName());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
