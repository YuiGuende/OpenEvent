package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.Message;
import com.group02.openevent.ai.util.ConfigLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 *
 * @author Admin
 */
@Service
public class LLM {

    private final String apiKey; // Không phải static final nữa
    private final String endpoint; // Không phải static final nữa

    public LLM(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.endpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + this.apiKey;
    }
    public String generateResponse(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be null or empty");
        }
        
        HttpURLConnection conn = null;
        int retryCount = 0;
        final int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try {
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000); // 10 seconds
                conn.setReadTimeout(30000); // 30 seconds

                // Build the request JSON
                List<Map<String, String>> parts = new ArrayList<>();
                for (Message msg : messages) {
                    if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                        Map<String, String> part = new HashMap<>();
                        part.put("text", msg.getContent());
                        parts.add(part);
                    }
                }

                Map<String, Object> content = new HashMap<>();
                content.put("role", "user");
                content.put("parts", parts);

                List<Map<String, Object>> contents = new ArrayList<>();
                contents.add(content);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("contents", contents);

                // Convert to JSON
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(requestBody);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Check response code
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    String errorBody = "";
                    try (Scanner errorScanner = new Scanner(conn.getErrorStream())) {
                        while (errorScanner.hasNext()) {
                            errorBody += errorScanner.nextLine();
                        }
                    }
                    
                    if (responseCode == 429) { // Rate limit
                        if (retryCount < maxRetries - 1) {
                            Thread.sleep(1000 * (retryCount + 1)); // Exponential backoff
                            retryCount++;
                            continue;
                        }
                    }
                    
                    throw new RuntimeException("Gemini API error: HTTP " + responseCode + " - " + errorBody);
                }

                // Read response
                Scanner scanner = new Scanner(conn.getInputStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                // Parse response
                Map<String, Object> result = mapper.readValue(response.toString(), Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
                
                if (candidates == null || candidates.isEmpty()) {
                    throw new RuntimeException("No candidates in Gemini response");
                }
                
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> message = (Map<String, Object>) firstCandidate.get("content");
                List<Map<String, Object>> contentParts = (List<Map<String, Object>>) message.get("parts");

                if (contentParts == null || contentParts.isEmpty()) {
                    throw new RuntimeException("No content parts in Gemini response");
                }

                return (String) contentParts.get(0).get("text");

            } catch (java.net.SocketTimeoutException e) {
                if (retryCount < maxRetries - 1) {
                    retryCount++;
                    continue;
                }
                throw new RuntimeException("Gemini API timeout after " + maxRetries + " attempts", e);
            } catch (java.net.ConnectException e) {
                if (retryCount < maxRetries - 1) {
                    retryCount++;
                    try { Thread.sleep(1000 * retryCount); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                throw new RuntimeException("Cannot connect to Gemini API", e);
            } catch (Exception e) {
                if (retryCount < maxRetries - 1) {
                    retryCount++;
                    try { Thread.sleep(1000 * retryCount); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                throw new RuntimeException("Gemini API error after " + maxRetries + " attempts: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        
        throw new RuntimeException("Max retries exceeded for Gemini API");
    }
}

