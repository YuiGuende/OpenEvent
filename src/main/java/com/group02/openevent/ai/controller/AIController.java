package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.dto.CustomerResponse;
import com.group02.openevent.ai.service.EventAIAgent;
import com.group02.openevent.ai.util.SessionManager;
import com.group02.openevent.model.ai.ChatHistory;
import com.group02.openevent.repository.IChatHistoryRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller chính để xử lý các request AI
 * @author Admin
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@Tag(name = "AI Controller", description = "API for AI-powered event management")
@Slf4j
public class AIController {

    private final IChatHistoryRepo chatHistoryRepo;

    public AIController(IChatHistoryRepo chatHistoryRepo) {
        this.chatHistoryRepo = chatHistoryRepo;
    }

    /**
     * Xử lý input từ người dùng và trả về response từ AI
     * @param request Map chứa message và userId
     * @param session HTTP session
     * @param response HTTP response
     * @return ResponseEntity chứa response từ AI
     */
    @Operation(summary = "Chat with AI", description = "Send a message to AI and get response")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully processed chat message"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/chat")
    public ResponseEntity<CustomerResponse> chat(
            @Parameter(description = "Chat request containing message and userId") 
            @RequestBody Map<String, Object> request, 
            HttpSession session, 
            HttpServletResponse response) {
        try {
            String message = (String) request.get("message");
            Integer userId = (Integer) request.get("userId");
            
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new CustomerResponse("❌ Tin nhắn không được để trống", false));
            }
            
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                    .body(new CustomerResponse("❌ User ID không hợp lệ", false));
            }

            // Lấy hoặc tạo session ID
            String sessionId = (String) session.getAttribute("sessionId");
            if (sessionId == null) {
                sessionId = "SESSION_" + System.currentTimeMillis() + "_" + userId;
                session.setAttribute("sessionId", sessionId);
            }

            // Lấy AI Agent cho session này với error handling
            EventAIAgent agent;
            try {
                agent = SessionManager.getOrCreate(sessionId);
            } catch (Exception e) {
                log.error("Session management error: {}", e.getMessage(), e);
                return ResponseEntity.internalServerError()
                    .body(new CustomerResponse("❌ Lỗi quản lý session. Vui lòng thử lại.", false));
            }
            
            // Xử lý input và lấy response từ AI
            String aiResponse;
            try {
                aiResponse = agent.processUserInput(message, userId, response);
            } catch (Exception e) {
                log.error("AI processing error: {}", e.getMessage(), e);
                return ResponseEntity.internalServerError()
                    .body(new CustomerResponse("❌ Lỗi xử lý AI. Vui lòng thử lại sau.", false));
            }
            
            // Lưu lịch sử chat vào database
            try {
                saveChatHistory(userId, message, aiResponse, sessionId);
            } catch (Exception e) {
                log.warn("Failed to save chat history: {}", e.getMessage());
                // Continue execution even if history saving fails
            }
            
            // Kiểm tra nếu cần reload frontend
            boolean shouldReload = aiResponse.contains("__RELOAD__");
            if (shouldReload) {
                aiResponse = aiResponse.replace("__RELOAD__", "").trim();
            }
            
            return ResponseEntity.ok(new CustomerResponse(aiResponse, shouldReload));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new CustomerResponse("❌ Dữ liệu đầu vào không hợp lệ", false));
        } catch (Exception e) {
            log.error("Unexpected error in AI chat: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new CustomerResponse("❌ Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", false));
        }
    }

    /**
     * Health check cho AI system
     * @return ResponseEntity chứa trạng thái hệ thống
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now().toString());
            health.put("sessionStats", SessionManager.getSessionStats());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "DOWN");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.internalServerError().body(errorHealth);
        }
    }

    /**
     * Lấy lời chào từ AI Agent
     * @param session HTTP session
     * @return ResponseEntity chứa lời chào
     */
    @GetMapping("/greeting")
    public ResponseEntity<CustomerResponse> getGreeting(HttpSession session) {
        try {
            String sessionId = (String) session.getAttribute("sessionId");
            if (sessionId == null) {
                sessionId = "SESSION_" + System.currentTimeMillis();
                session.setAttribute("sessionId", sessionId);
            }
            
            EventAIAgent agent = SessionManager.getOrCreate(sessionId);
            String greeting = agent.getGreeting();
            
            return ResponseEntity.ok(new CustomerResponse(greeting, false));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new CustomerResponse("❌ Đã xảy ra lỗi khi lấy lời chào", false));
        }
    }

    /**
     * Lấy tóm tắt cuộc trò chuyện
     * @param session HTTP session
     * @return ResponseEntity chứa tóm tắt
     */
    @GetMapping("/summary")
    public ResponseEntity<CustomerResponse> getConversationSummary(HttpSession session) {
        try {
            String sessionId = (String) session.getAttribute("sessionId");
            if (sessionId == null) {
                return ResponseEntity.ok(new CustomerResponse("📭 Chưa có cuộc trò chuyện nào", false));
            }
            
            EventAIAgent agent = SessionManager.getOrCreate(sessionId);
            String summary = agent.getConversationSummary();
            
            return ResponseEntity.ok(new CustomerResponse(summary, false));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new CustomerResponse("❌ Đã xảy ra lỗi khi lấy tóm tắt", false));
        }
    }

    /**
     * Kiểm tra xem có nên kết thúc cuộc trò chuyện không
     * @param request Map chứa message
     * @param session HTTP session
     * @return ResponseEntity chứa kết quả kiểm tra
     */
    @PostMapping("/should-end")
    public ResponseEntity<Map<String, Object>> shouldEndConversation(@RequestBody Map<String, String> request,
                                                                     HttpSession session) {
        try {
            String message = request.get("message");
            String sessionId = (String) session.getAttribute("sessionId");
            
            boolean shouldEnd = false;
            if (sessionId != null) {
                EventAIAgent agent = SessionManager.getOrCreate(sessionId);
                shouldEnd = agent.shouldEndConversation(message);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("shouldEnd", shouldEnd);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("shouldEnd", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Lưu lịch sử chat vào database
     */
    private void saveChatHistory(Integer userId, String userMessage, String aiResponse, String sessionId) {
        try {
            // Lưu tin nhắn của user
            ChatHistory userChat = new ChatHistory();
            userChat.setUserId(userId);
            userChat.setMessage(userMessage);
            userChat.setRole("user");
            userChat.setSessionId(sessionId);
            userChat.setTimestamp(LocalDateTime.now());
            chatHistoryRepo.save(userChat);
            
            // Lưu phản hồi của AI
            ChatHistory aiChat = new ChatHistory();
            aiChat.setUserId(userId);
            aiChat.setMessage(aiResponse);
            aiChat.setRole("assistant");
            aiChat.setSessionId(sessionId);
            aiChat.setTimestamp(LocalDateTime.now());
            chatHistoryRepo.save(aiChat);
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lưu lịch sử chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
