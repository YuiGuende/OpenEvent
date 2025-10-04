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

            // Lấy AI Agent cho session này
            EventAIAgent agent = SessionManager.getOrCreate(sessionId);
            
            // Xử lý input và lấy response từ AI
            String aiResponse = agent.processUserInput(message, userId, response);
            
            // Lưu lịch sử chat vào database
            saveChatHistory(userId, message, aiResponse, sessionId);
            
            // Kiểm tra nếu cần reload frontend
            boolean shouldReload = aiResponse.contains("__RELOAD__");
            if (shouldReload) {
                aiResponse = aiResponse.replace("__RELOAD__", "").trim();
            }
            
            return ResponseEntity.ok(new CustomerResponse(aiResponse, shouldReload));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new CustomerResponse("❌ Đã xảy ra lỗi khi xử lý yêu cầu: " + e.getMessage(), false));
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
