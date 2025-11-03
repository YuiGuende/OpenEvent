package com.group02.openevent.ai.exception;

/**
 * Error codes for AI system
 */
public class AIErrorCodes {
    
    // LLM Errors
    public static final String LLM_API_ERROR = "LLM_001";
    public static final String LLM_TIMEOUT = "LLM_002";
    public static final String LLM_INVALID_RESPONSE = "LLM_003";
    
    // Embedding Errors
    public static final String EMBEDDING_API_ERROR = "EMB_001";
    public static final String EMBEDDING_TIMEOUT = "EMB_002";
    public static final String EMBEDDING_INVALID_INPUT = "EMB_003";
    
    // Qdrant Errors
    public static final String QDRANT_CONNECTION_ERROR = "QDR_001";
    public static final String QDRANT_SEARCH_ERROR = "QDR_002";
    public static final String QDRANT_UPSERT_ERROR = "QDR_003";
    
    // Session Errors
    public static final String SESSION_NOT_FOUND = "SES_001";
    public static final String SESSION_EXPIRED = "SES_002";
    public static final String SESSION_INVALID = "SES_003";
    
    // General Errors
    public static final String INVALID_INPUT = "GEN_001";
    public static final String SERVICE_UNAVAILABLE = "GEN_002";
    public static final String UNKNOWN_ERROR = "GEN_003";
}

