package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.service.EventAIAgent;
import com.group02.openevent.ai.util.SessionManager;
import com.group02.openevent.model.ai.ChatHistory;
import com.group02.openevent.repository.IChatHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * Controller để xử lý các chức năng chat và lịch sử hội thoại
 * @author Admin
 */
@RestController
@RequestMapping("/api/ai/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    
    @Autowired
    private IChatHistoryRepo chatHistoryRepo;

    /**
     * Lấy lịch sử chat của một user
     * @param userId ID của user
     * @param sessionId ID của session (optional)
     * @return ResponseEntity chứa danh sách lịch sử chat
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ChatHistory>> getChatHistory(@PathVariable Integer userId,
                                                           @RequestParam(required = false) String sessionId) {
        try {
            List<ChatHistory> chatHistory;
            
            if (sessionId != null && !sessionId.isEmpty()) {
                // Lấy lịch sử của session cụ thể
                chatHistory = chatHistoryRepo.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId);
            } else {
                // Lấy tất cả lịch sử của user
                chatHistory = chatHistoryRepo.findByUserIdOrderByTimestampAsc(userId);
            }
            
            return ResponseEntity.ok(chatHistory);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy danh sách các session của user
     * @param userId ID của user
     * @return ResponseEntity chứa danh sách session
     */
    @GetMapping("/sessions/{userId}")
    public ResponseEntity<List<String>> getUserSessions(@PathVariable Integer userId) {
        try {
            List<String> sessions = chatHistoryRepo.findDistinctSessionIdsByUserId(userId);
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xóa lịch sử chat của một session
     * @param userId ID của user
     * @param sessionId ID của session
     * @return ResponseEntity chứa kết quả
     */
    @DeleteMapping("/history/{userId}/{sessionId}")
    public ResponseEntity<Map<String, String>> clearChatHistory(@PathVariable Integer userId,
                                                               @PathVariable String sessionId) {
        try {
            chatHistoryRepo.deleteByUserIdAndSessionId(userId, sessionId);
            
            // Xóa session khỏi SessionManager
            SessionManager.remove(sessionId);
            
            Map<String, String> result = Map.of("message", "✅ Đã xóa lịch sử chat thành công");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> result = Map.of("error", "❌ Lỗi khi xóa lịch sử chat: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Lấy thông tin session hiện tại
     * @param session HTTP session
     * @return ResponseEntity chứa thông tin session
     */
    @GetMapping("/session-info")
    public ResponseEntity<Map<String, Object>> getSessionInfo(HttpSession session) {
        try {
            String sessionId = (String) session.getAttribute("sessionId");
            
            Map<String, Object> sessionInfo = Map.of(
                "sessionId", sessionId != null ? sessionId : "N/A",
                "isNew", sessionId == null,
                "createdAt", session.getCreationTime(),
                "lastAccessed", session.getLastAccessedTime()
            );
            
            return ResponseEntity.ok(sessionInfo);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorInfo = Map.of("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorInfo);
        }
    }

    /**
     * Khởi tạo session mới
     * @param session HTTP session
     * @return ResponseEntity chứa thông tin session mới
     */
    @PostMapping("/new-session")
    public ResponseEntity<Map<String, String>> createNewSession(HttpSession session) {
        try {
            // Tạo session ID mới
            String newSessionId = "SESSION_" + System.currentTimeMillis();
            session.setAttribute("sessionId", newSessionId);
            
            // Tạo AI Agent mới cho session này (sử dụng default agent)
            EventAIAgent newAgent = SessionManager.get(newSessionId);
            SessionManager.put(newSessionId, newAgent);
            
            Map<String, String> result = Map.of(
                "sessionId", newSessionId,
                "message", "✅ Đã tạo session mới thành công"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> result = Map.of("error", "❌ Lỗi khi tạo session mới: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Lấy thống kê chat của user
     * @param userId ID của user
     * @return ResponseEntity chứa thống kê
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getChatStats(@PathVariable Integer userId) {
        try {
            List<ChatHistory> allChats = chatHistoryRepo.findByUserIdOrderByTimestampAsc(userId);
            
            long totalMessages = allChats.size();
            long userMessages = allChats.stream()
                .filter(chat -> "user".equals(chat.getRole()))
                .count();
            long aiMessages = allChats.stream()
                .filter(chat -> "assistant".equals(chat.getRole()))
                .count();
            
            List<String> sessions = chatHistoryRepo.findDistinctSessionIdsByUserId(userId);
            int totalSessions = sessions.size();
            
            // Tìm session có nhiều tin nhắn nhất
            String mostActiveSession = sessions.stream()
                .mapToLong(sessionId -> 
                    chatHistoryRepo.countByUserIdAndSessionId(userId, sessionId))
                .max()
                .orElse(0) > 0 ? 
                sessions.get(0) : null;
            
            Map<String, Object> stats = Map.of(
                "totalMessages", totalMessages,
                "userMessages", userMessages,
                "aiMessages", aiMessages,
                "totalSessions", totalSessions,
                "mostActiveSession", mostActiveSession != null ? mostActiveSession : "N/A",
                "averageMessagesPerSession", totalSessions > 0 ? totalMessages / totalSessions : 0
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorStats = Map.of("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorStats);
        }
    }
}
