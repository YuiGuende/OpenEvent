package com.group02.openevent.ai.qdrant.service;

import com.group02.openevent.ai.qdrant.model.ActionType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.ai.service.EventVectorSearchService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service để phân loại ý định người dùng sử dụng vector similarity
 * @author Admin
 */
@Service
@Slf4j
public class VectorIntentClassifier {

    private final Map<String, float[]> ticketInfoExampleVectors = new HashMap<>();
    private final QdrantService qdrantService;
    private final EmbeddingService embeddingService;
    private final EventVectorSearchService eventVectorSearchService;

    public VectorIntentClassifier(QdrantService qdrantService, EmbeddingService embeddingService,EventVectorSearchService eventVectorSearchService) {
        this.qdrantService = qdrantService;
        this.embeddingService = embeddingService;
        this.eventVectorSearchService = eventVectorSearchService;
    }

    /**
     * Trích xuất tên sự kiện từ câu nói của người dùng bằng cách sử dụng EventVectorSearchService.
     * Đây là phương pháp đúng đắn để tách biệt việc tìm kiếm thực thể.
     *
     * @param userInput Câu nói của người dùng.
     * @return Tên của sự kiện khớp nhất, hoặc chuỗi rỗng nếu không tìm thấy.
     */
    public String extractEventName(String userInput) {
        System.out.println("🔍 DEBUG: Extracting event name using EventVectorSearchService for: '" + userInput + "'");
        try {
            // Gọi service chuyên dụng để tìm kiếm sự kiện, chỉ lấy 1 kết quả tốt nhất
            List<Event> foundEvents = eventVectorSearchService.searchEvents(userInput, 0L, 1); // userId=0 vì chưa cần lọc

            if (foundEvents != null && !foundEvents.isEmpty()) {
                // Lấy tên từ sự kiện đầu tiên (khớp nhất)
                Event bestMatch = foundEvents.get(0);
                String eventName = bestMatch.getTitle();
                System.out.println("✅ DEBUG: Extracted event name: '" + eventName + "'");
                return eventName;
            } else {
                System.out.println("❌ DEBUG: EventVectorSearchService found no matching events.");
                return "";
            }
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất tên sự kiện: {}", e.getMessage(), e);
            return ""; // Trả về rỗng nếu có lỗi
        }
    }

    @PostConstruct
    public void initializeExampleVectors() {
        log.info("Đang khởi tạo và tính toán trước các vector mẫu cho việc hỏi thông tin vé...");
        String[] ticketInfoExamples = {
                "giá vé sự kiện là bao nhiêu",
                "vé thường có giá bao nhiêu",
                "vé vip giá bao nhiêu tiền",
                "có những loại vé nào",
                "vé nào còn sẵn",
                "thông tin vé sự kiện",
                "chi tiết giá vé",
                "coi lại vé thường",
                "xem giá vé vip",
                "bao nhiêu tiền một vé",
                "giá cả các loại vé",
                "vé còn lại bao nhiêu"
        };

        for (String example : ticketInfoExamples) {
            try {
                float[] vector = embeddingService.getEmbedding(example);
                this.ticketInfoExampleVectors.put(example, vector);
            } catch (Exception e) {
                log.error("Lỗi khi tạo embedding cho câu mẫu: '{}'. Bỏ qua câu này.", example, e);
            }
        }
        log.info("Hoàn tất khởi tạo {} vector mẫu.", this.ticketInfoExampleVectors.size());
    }

    public ActionType classifyIntent(String userInput, float[] userVector) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return ActionType.UNKNOWN;
            }

            // Kiểm tra câu hỏi về thông tin vé trước
            if (isTicketInfoQuery(userInput, userVector)) {
                return ActionType.QUERY_TICKET_INFO;
            }

            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(userVector, 3);

            if (results == null || results.isEmpty()) {
                return ActionType.UNKNOWN;
            }

            Map<String, Object> bestMatch = results.get(0);
            if (bestMatch == null) {
                return ActionType.UNKNOWN;
            }

            // Defensive cast for score
            Number scoreNum = (Number) bestMatch.getOrDefault("score", 0);
            double score = scoreNum.doubleValue();

            if (score < 0.80) {
                return ActionType.UNKNOWN;
            }

            Map<String, Object> payload = (Map<String, Object>) bestMatch.get("payload");
            if (payload == null) {
                return ActionType.UNKNOWN;
            }
            
            String typeString = (String) payload.getOrDefault("type", "unknown");
            return ActionType.fromString(typeString);

        } catch (Exception e) {
            e.printStackTrace();
            return ActionType.UNKNOWN; // Return UNKNOWN instead of ERROR for graceful fallback
        }
    }
    public String classifyWeather(String userInput,  float[] userVector) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return "UNKNOWN";
            }

            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(userVector, 3);
            System.out.println(results);
            
            if (results == null || results.isEmpty()) {
                return "EMPTY";
            }

            Map<String, Object> bestMatch = results.get(0);
            if (bestMatch == null) {
                return "UNKNOWN";
            }

            Number scoreNum = (Number) bestMatch.getOrDefault("score", 0);
            double score = scoreNum.doubleValue();
            System.out.println(score);
            
            if (score < 0.6) {
                return "<0.6";
            }

            Map<String, Object> payload = (Map<String, Object>) bestMatch.get("payload");
            if (payload == null) {
                return "UNKNOWN";
            }
            
            String typeString = (String) payload.getOrDefault("type", "unknown");
            return typeString;

        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }
    public String classifyEventTitle(String userInput, float[] queryVector) {
        try {
            if (queryVector == null) {
                return "";
            }

            // 1. CHUẨN BỊ BỘ LỌC THỜI GIAN (Quan trọng: Chỉ tìm sự kiện sắp tới/đang diễn ra)
            long currentTimestamp = java.time.Instant.now().getEpochSecond();

            Map<String, Object> timeFilter = Map.of(
                    "key", "endsAt", // Giả định bạn lưu 'endsAt' dưới dạng timestamp trong payload
                    "range", Map.of("gt", currentTimestamp) // Chỉ tìm sự kiện chưa kết thúc
            );

            Map<String, Object> filter = Map.of(
                    "must", List.of(
                            // Chỉ tìm kiếm các vector sự kiện
                            Map.of("key", "kind", "match", Map.of("value", "event")),
                            timeFilter
                    )
            );

            // 2. TÌM KIẾM VECTOR SỰ KIỆN VỚI FILTER
            // Cần phương thức searchSimilarVectorsWithFilter(vector, limit, filter) trong QdrantService
            List<Map<String, Object>> results = qdrantService.searchSimilarVectorsWithFilter(queryVector, 1, filter);

            if (results == null || results.isEmpty()) {
                return ""; // Không tìm thấy sự kiện sắp tới nào tương đồng
            }

            Map<String, Object> bestMatch = results.get(0);
            Number scoreNum = (Number) bestMatch.getOrDefault("score", 0);
            double score = scoreNum.doubleValue();

            // 3. ĐIỀU KIỆN NGƯỠNG ĐIỂM
            // Chỉ lấy kết quả nếu điểm tương đồng đủ cao (ví dụ: > 0.85)
            if (score < 0.85) {
                return "";
            }

            // 4. TRẢ VỀ TIÊU ĐỀ SỰ KIỆN TỪ PAYLOAD
            Map<String, Object> payload = (Map<String, Object>) bestMatch.get("payload");
            if (payload != null) {
                // Giả định tiêu đề sự kiện được lưu trong payload dưới key "title"
                String title = (String) payload.getOrDefault("title", "");
                return title.trim();
            }

            return "";

        } catch (Exception e) {
            log.error("Error during classifyEventTitle (Vector Search): {}", e.getMessage(), e);
            return "";
        }
    }
    public String classifyToolEvent(String userInput, float[] userVector) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return "UNKNOWN";
            }

            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(userVector, 3);
            System.out.println(results);
            
            if (results == null || results.isEmpty()) {
                return "";
            }

            Map<String, Object> bestMatch = results.get(0);
            if (bestMatch == null) {
                return "UNKNOWN";
            }

            Number scoreNum = (Number) bestMatch.getOrDefault("score", 0);
            double score = scoreNum.doubleValue();
            System.out.println(score);
            
            if (score < 0.8) {
                return "<0.8";
            }

            Map<String, Object> payload = (Map<String, Object>) bestMatch.get("payload");
            if (payload == null) {
                return "UNKNOWN";
            }
            
            String typeString = (String) payload.getOrDefault("toolName", "unknown");
            return typeString;

        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }
    
    /**
     * Extract event name from buy ticket intent using AI
     */
    public String extractEventNameFromBuyTicketIntent(String userInput, float[] userVector) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return "";
            }
            
            // Use LLM to extract event name more intelligently
            String prompt = String.format("""
                Từ câu sau, hãy trích xuất tên sự kiện mà người dùng muốn mua vé:
                Câu: "%s"
                
                Chỉ trả về tên sự kiện, không có từ nào khác.
                Ví dụ: "Music Night", "Tech Conference", "Workshop AI"
                
                Nếu không tìm thấy tên sự kiện rõ ràng, trả về "UNKNOWN".
                """, userInput);

            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(userVector, 5);
            
            if (results != null && !results.isEmpty()) {
                // Look for event-related patterns in results
                for (Map<String, Object> result : results) {
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    if (payload != null) {
                        String eventName = (String) payload.get("eventName");
                        if (eventName != null && !eventName.trim().isEmpty()) {
                            return eventName.trim();
                        }
                    }
                }
            }
            
            // Fallback: Use embedding-based extraction
            return extractEventNameWithEmbedding(userInput);
            
        } catch (Exception e) {
            e.printStackTrace();
            return extractEventNameWithEmbedding(userInput);
        }
    }
    
    /**
     * Fallback method using embedding similarity to extract event name
     */
    private String extractEventNameWithEmbedding(String userInput) {
        try {
            System.out.println("🔍 DEBUG: Extracting event name with embedding for: '" + userInput + "'");
            
            // Tạo embedding cho user input
            float[] userVector = embeddingService.getEmbedding(userInput);
            
            // Tìm kiếm sự kiện trong Qdrant
            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(userVector, 5);
            
            if (results != null && !results.isEmpty()) {
                for (Map<String, Object> result : results) {
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    if (payload != null) {
                        String eventName = (String) payload.get("eventName");
                        if (eventName != null && !eventName.trim().isEmpty()) {
                            System.out.println("✅ DEBUG: Found event name via embedding: '" + eventName + "'");
                            return eventName.trim();
                        }
                    }
                }
            }
            
            // Fallback: Simple regex extraction
            return extractEventNameWithRegex(userInput);
            
        } catch (Exception e) {
            System.out.println("❌ DEBUG: Error in embedding event name extraction: " + e.getMessage());
            return extractEventNameWithRegex(userInput);
        }
    }
    
    /**
     * Fallback method using regex patterns
     */
    private String extractEventNameWithRegex(String userInput) {
        // Common patterns for event names in buy ticket requests
        String[] patterns = {
            "(?:mua vé|mua ve|đăng ký|đăng ky|tham gia|đặt vé|dat ve|book vé|order vé)\\s+(?:sự kiện|su kien|event)?\\s*:?\\s*([^,]+)",
            "(?:cho|về|tại)\\s+([^,]+?)(?:\\s|$)",
            "(?:tên|ten)\\s*(?:sự kiện|su kien|event)?\\s*:?\\s*([^,]+)",
            "\"([^\"]+)\"",
            "'([^']+)'"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(userInput);
            if (m.find()) {
                String eventName = m.group(1).trim();
                // Clean up common words
                eventName = eventName.replaceAll("(?i)\\b(sự kiện|su kien|event|cho|về|tại)\\b", "").trim();
                if (!eventName.isEmpty() && eventName.length() > 2) {
                    return eventName;
                }
            }
        }
        
        return "";
    }
    
    /**
     * Classify user intent as CONFIRM_ORDER, CANCEL_ORDER, or UNKNOWN
     * for order confirmation scenarios
     */
    public ActionType classifyConfirmIntent(String userInput, float[] userVector) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return ActionType.UNKNOWN;
            }

            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(userVector, 3);
            
            if (results != null && !results.isEmpty()) {
                Map<String, Object> bestMatch = results.get(0);
                if (bestMatch != null) {
                    Number scoreNum = (Number) bestMatch.getOrDefault("score", 0);
                    double score = scoreNum.doubleValue();
                    
                    if (score >= 0.75) { // High confidence threshold for confirm/cancel
                        Map<String, Object> payload = (Map<String, Object>) bestMatch.get("payload");
                        if (payload != null) {
                            String intentType = (String) payload.getOrDefault("intentType", "unknown");
                            return ActionType.fromString(intentType);
                        }
                    }
                }
            }
            
            // Fallback: Pattern-based classification with improved regex
            return classifyConfirmIntentWithPatterns(userInput);
            
        } catch (Exception e) {
            e.printStackTrace();
            return classifyConfirmIntentWithPatterns(userInput);
        }
    }
    
    /**
     * Fallback pattern-based classification for confirm/cancel intents
     */
    private ActionType classifyConfirmIntentWithPatterns(String userInput) {
        String lowerInput = userInput.toLowerCase().trim();
        
        // Strong confirm patterns (high priority)
        String[] strongConfirmPatterns = {
            "có", "co", "yes", "ok", "okay", "đồng ý", "dong y",
            "xác nhận", "xac nhan", "confirm", "agree", "accept",
            "tiếp tục", "tiep tuc", "continue", "proceed",
            "tôi đồng ý", "toi dong y", "i agree", "i confirm",
            "chắc chắn", "chac chan", "sure", "definitely"
        };
        
        // Strong cancel patterns (high priority)
        String[] strongCancelPatterns = {
            "không", "khong", "no", "cancel", "hủy", "huy",
            "từ chối", "tu choi", "refuse", "reject", "decline",
            "dừng lại", "dung lai", "stop", "abort", "quit",
            "tôi không muốn", "toi khong muon", "i don't want",
            "không đồng ý", "khong dong y", "disagree"
        };
        
        // Check for strong confirm patterns
        for (String pattern : strongConfirmPatterns) {
            if (lowerInput.contains(pattern)) {
                return ActionType.CONFIRM_ORDER;
            }
        }
        
        // Check for strong cancel patterns
        for (String pattern : strongCancelPatterns) {
            if (lowerInput.contains(pattern)) {
                return ActionType.CANCEL_ORDER;
            }
        }
        
        // Weak patterns (lower priority, context dependent)
        String[] weakConfirmPatterns = {
            "tiến hành", "tien hanh", "go ahead", "let's go",
            "được", "duoc", "fine", "good", "alright"
        };
        
        String[] weakCancelPatterns = {
            "thôi", "thoi", "never mind", "forget it",
            "không cần", "khong can", "not needed"
        };
        
        // Check weak patterns only if no strong pattern found
        for (String pattern : weakConfirmPatterns) {
            if (lowerInput.contains(pattern)) {
                return ActionType.CONFIRM_ORDER;
            }
        }
        
        for (String pattern : weakCancelPatterns) {
            if (lowerInput.contains(pattern)) {
                return ActionType.CANCEL_ORDER;
            }
        }
        
        // Default to unknown if no clear pattern
        return ActionType.UNKNOWN;
    }

    /**
     * Kiểm tra xem user input có phải là câu hỏi về thông tin vé không sử dụng embedding similarity
     */
    public boolean isTicketInfoQuery(String userInput, float[] userVector) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }
        
        System.out.println("🔍 DEBUG: Checking ticket info query with embedding for: '" + userInput + "'");
        
        try {

            
            // Tính similarity với từng câu mẫu
            double maxSimilarity = 0.0;
            String bestMatch = "";

            for (Map.Entry<String, float[]> entry : this.ticketInfoExampleVectors.entrySet()) {
                String exampleText = entry.getKey();
                float[] exampleVector = entry.getValue(); // Lấy vector đã được tính sẵn

                double similarity = cosineSimilarity(userVector, exampleVector);

                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    bestMatch = exampleText;
                }
            }
            
            System.out.println("🎯 DEBUG: Best match: '" + bestMatch + "' with similarity: " + maxSimilarity);
            
            // Ngưỡng similarity để xác định là câu hỏi về thông tin vé
            double threshold = 0.75;
            
            if (maxSimilarity >= threshold) {
                System.out.println("✅ DEBUG: Detected ticket info query with similarity: " + maxSimilarity);
                return true;
            } else {
                System.out.println("❌ DEBUG: Not a ticket info query. Max similarity: " + maxSimilarity + " (threshold: " + threshold + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ DEBUG: Error in embedding similarity check: " + e.getMessage());
            System.out.println("🔄 DEBUG: Falling back to keyword matching...");
            
            // Fallback: sử dụng keyword matching
            String lowerInput = userInput.toLowerCase().trim();
            String[] ticketInfoKeywords = {
                "giá vé", "giá tiền", "bao nhiêu tiền", "giá cả",
                "vé thường", "vé vip", "vé early bird", "loại vé",
                "có những loại vé nào", "vé nào có sẵn", "vé còn lại",
                "coi lại vé", "xem vé", "thông tin vé", "chi tiết vé"
            };
            
            for (String keyword : ticketInfoKeywords) {
                if (lowerInput.contains(keyword)) {
                    System.out.println("✅ DEBUG: Found ticket info keyword: '" + keyword + "'");
                    return true;
                }
            }
            
            System.out.println("❌ DEBUG: No ticket info keywords found");
            return false;
        }
    }
    
    /**
     * Tính cosine similarity giữa hai vector
     */
    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

