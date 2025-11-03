package com.group02.openevent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
@Tag(name = "Test Controller", description = "Simple test endpoints to verify API functionality")
public class TestController {

    @Operation(summary = "Health Check", description = "Check if the API is running")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "API is running successfully")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "OpenEvent API is running!");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Echo Message", description = "Echo back the message sent")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message echoed successfully")
    })
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(
            @Parameter(description = "Message to echo back") 
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("originalMessage", request.get("message"));
        response.put("echoedMessage", "Echo: " + request.get("message"));
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Server Info", description = "Get basic server information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Server info retrieved successfully")
    })
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("applicationName", "OpenEvent");
        response.put("version", "1.0.0");
        response.put("environment", "development");
        response.put("timestamp", LocalDateTime.now());
        response.put("features", new String[]{"AI Chat", "Event Management", "Weather Integration"});
        return ResponseEntity.ok(response);
    }
}
