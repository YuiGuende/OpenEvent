package com.group02.openevent.services.ai;

import com.group02.openevent.ai.service.EventAIAgent;
import com.group02.openevent.ai.util.SessionManager;
import com.group02.openevent.dto.ai.*;
import com.group02.openevent.models.ai.ChatMessage;
import com.group02.openevent.models.ai.ChatSession;
import com.group02.openevent.repository.ai.ChatMessageRepo;
import com.group02.openevent.repository.ai.ChatSessionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing chat sessions and messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {
    
    private final ChatSessionRepo chatSessionRepo;
    private final ChatMessageRepo chatMessageRepo;
    
    /**
     * List all sessions for a user
     */
    public List<SessionItem> list(Long userId) {
        List<ChatSession> sessions = chatSessionRepo.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream()
                .map(this::convertToSessionItem)
                .toList();
    }
    
    /**
     * Create a new chat session
     */
    @Transactional
    public NewSessionRes create(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        ChatSession savedSession = chatSessionRepo.save(session);
        
        return new NewSessionRes(savedSession.getSessionId(), savedSession.getTitle());
    }
    
    /**
     * Get chat history for a session
     */
    public List<ChatMessage> history(Long userId, String sessionId) {
        return chatMessageRepo.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId);
    }
    
    /**
     * Process chat message
     */
    @Transactional
    public ChatReply chat(ChatRequest request) {
        try {
            // Save user message
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSessionId(request.sessionId());
            userMessage.setUserId(request.userId());
            userMessage.setMessage(request.message());
            userMessage.setIsFromUser(true);
            userMessage.setTimestamp(LocalDateTime.now());
            chatMessageRepo.save(userMessage);
            
            // Generate AI response (simplified)
            String aiResponse = generateAIResponse(request.message(), request.userId(), request.sessionId());
            
            // Save AI response
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSessionId(request.sessionId());
            aiMessage.setUserId(request.userId());
            aiMessage.setMessage(aiResponse);
            aiMessage.setIsFromUser(false);
            aiMessage.setTimestamp(LocalDateTime.now());
            chatMessageRepo.save(aiMessage);
            
            return new ChatReply(aiResponse, false, LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error processing chat: {}", e.getMessage(), e);
            return new ChatReply("❌ Đã xảy ra lỗi khi xử lý tin nhắn", false, LocalDateTime.now());
        }
    }
    
    /**
     * Convert ChatSession to SessionItem
     */
    private SessionItem convertToSessionItem(ChatSession session) {
        return new SessionItem(
                session.getSessionId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
    
    /**
     * Generate AI response using EventAIAgent
     */
    private String generateAIResponse(String userMessage, Long userId, String sessionId) {
        try {
            // Get or create AI agent for the session
            EventAIAgent agent = SessionManager.getOrCreate("default");
            
            // Generate response using the AI agent
            String response = agent.reply(userMessage, userId, sessionId);
            
            return response != null ? response : "🤖 Xin lỗi, tôi không thể tạo phản hồi lúc này. Vui lòng thử lại sau.";
            
        } catch (Exception e) {
            log.error("Error generating AI response: {}", e.getMessage(), e);
            return "🤖 Xin lỗi, đã xảy ra lỗi khi xử lý tin nhắn. Vui lòng thử lại sau.";
        }
    }
}