package com.group02.openevent.ai.qdrant.service;

import com.group02.openevent.ai.qdrant.model.ActionType;
import com.group02.openevent.ai.service.EmbeddingService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * Service để phân loại ý định người dùng sử dụng vector similarity
 * @author Admin
 */
@Service
public class VectorIntentClassifier {

    private final QdrantService qdrantService;
    private final EmbeddingService embeddingService;

    public VectorIntentClassifier(QdrantService qdrantService, EmbeddingService embeddingService) {
        this.qdrantService = qdrantService;
        this.embeddingService = embeddingService;
    }

    public ActionType classifyIntent(String userInput) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return ActionType.UNKNOWN;
            }
            
            float[] queryVector = embeddingService.getEmbedding(userInput);
            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(queryVector, 3);

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
    public String classifyWeather(String userInput) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return "UNKNOWN";
            }
            
            float[] queryVector = embeddingService.getEmbedding(userInput);
            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(queryVector, 3);
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
    public String classifyToolEvent(String userInput) {
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                return "UNKNOWN";
            }
            
            float[] queryVector = embeddingService.getEmbedding(userInput);
            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(queryVector, 3);
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
}

