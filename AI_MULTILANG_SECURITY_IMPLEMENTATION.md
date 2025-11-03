# AI Multi-language Support & Security Implementation

## 📋 Overview

This document describes the implementation of multi-language support and security measures for the OpenEvent AI system.

## 🌍 Multi-language Support

### Features Implemented

#### 1. Language Detection Service
- **File**: `LanguageDetectionService.java`
- **Purpose**: Automatically detect user's language from input text
- **Supported Languages**: Vietnamese, English, Chinese, Japanese, Korean, French, German, Spanish
- **Detection Methods**:
  - Character pattern recognition
  - Keyword matching
  - Confidence scoring

#### 2. Translation Service
- **File**: `TranslationService.java`
- **Purpose**: Translate text between different languages
- **API Integration**: Google Translate API
- **Features**:
  - Caching for performance optimization
  - Async translation support
  - Fallback to original text on failure

#### 3. Language Model
- **File**: `Language.java`
- **Purpose**: Define supported languages and their properties
- **Properties**: Language code, name, native name

### API Endpoints

#### Enhanced AI Chat Controller
- **File**: `EnhancedAIController.java`
- **Endpoints**:
  - `POST /api/ai/chat` - Multi-language chat
  - `POST /api/ai/chat/detect-language` - Language detection
  - `POST /api/ai/chat/translate` - Text translation
  - `GET /api/ai/chat/languages` - Supported languages
  - `GET /api/ai/chat/rate-limit` - Rate limit information

### Usage Examples

#### 1. Chat with Language Detection
```bash
curl -X POST "http://localhost:8080/api/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, I want to create an event",
    "sessionId": 123,
    "language": "auto"
  }'
```

#### 2. Language Detection
```bash
curl -X POST "http://localhost:8080/api/ai/chat/detect-language" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Xin chào, tôi muốn tạo sự kiện"
  }'
```

#### 3. Text Translation
```bash
curl -X POST "http://localhost:8080/api/ai/chat/translate" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Hello world",
    "sourceLang": "en",
    "targetLang": "vi"
  }'
```

## 🔒 Security Measures

### Features Implemented

#### 1. Rate Limiting Service
- **File**: `RateLimitingService.java`
- **Purpose**: Prevent API abuse and ensure fair usage
- **Rate Limits**:
  - AI Chat: 20 requests/minute, 300/hour, 1000/day
  - Translation: 50 requests/minute, 1000/hour
  - General: 30 requests/minute, 500/hour, 2000/day

#### 2. AI Security Service
- **File**: `AISecurityService.java`
- **Purpose**: Input validation and sanitization
- **Security Features**:
  - SQL injection prevention
  - XSS protection
  - Command injection prevention
  - Path traversal protection
  - Input length validation
  - Character pattern validation

#### 3. Rate Limit Interceptor
- **File**: `RateLimitInterceptor.java`
- **Purpose**: Automatically apply rate limiting to AI endpoints
- **Features**:
  - Automatic rate limit checking
  - HTTP 429 responses for exceeded limits
  - Rate limit headers in responses

#### 4. Security Configuration
- **File**: `AISecurityConfig.java`
- **Purpose**: Configure security interceptors and settings

### Security Headers

The system automatically adds security headers to responses:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Remaining requests in current window
- `X-RateLimit-Reset`: Time when rate limit resets
- `Retry-After`: Seconds to wait before retrying (when rate limited)

### Input Validation

#### Supported Input Types
- `MESSAGE`: General chat messages
- `EVENT_TITLE`: Event titles
- `EVENT_DESCRIPTION`: Event descriptions
- `EMAIL`: Email addresses
- `PHONE`: Phone numbers
- `GENERAL`: General text input

#### Validation Rules
- Maximum input length: 5000 characters
- Maximum message length: 2000 characters
- Maximum event title: 200 characters
- Maximum event description: 2000 characters
- Pattern validation for emails and phone numbers
- Malicious content detection

## ⚙️ Configuration

### Application Properties

```properties
# Google Translate API Configuration
google.translate.api.key=${GOOGLE_TRANSLATE_API_KEY}

# AI Security Configuration
ai.security.enabled=true
ai.security.max-input-length=5000
ai.security.max-message-length=2000
ai.security.max-event-title-length=200
ai.security.max-event-description-length=2000

# Rate Limiting Configuration
ai.rate-limit.enabled=true
ai.rate-limit.requests-per-minute=30
ai.rate-limit.requests-per-hour=500
ai.rate-limit.requests-per-day=2000
ai.rate-limit.ai-requests-per-minute=20
ai.rate-limit.ai-requests-per-hour=300
ai.rate-limit.ai-requests-per-day=1000
ai.rate-limit.translation-requests-per-minute=50
ai.rate-limit.translation-requests-per-hour=1000

# Multi-language Configuration
ai.multilang.enabled=true
ai.multilang.default-language=vi
ai.multilang.auto-detect=true
ai.multilang.translation-cache-size=1000
```

### Environment Variables

Required environment variables:
- `GOOGLE_TRANSLATE_API_KEY`: Google Translate API key
- `GEMINI_API_KEY`: Google Gemini API key (existing)
- `HUGGINGFACE_TOKEN`: HuggingFace API token (existing)

## 🚀 Usage Guide

### 1. Setting Up Multi-language Support

1. **Configure API Keys**:
   ```bash
   export GOOGLE_TRANSLATE_API_KEY="your-google-translate-api-key"
   ```

2. **Enable Multi-language**:
   ```properties
   ai.multilang.enabled=true
   ai.multilang.auto-detect=true
   ```

3. **Test Language Detection**:
   ```bash
   curl -X POST "http://localhost:8080/api/ai/chat/detect-language" \
     -H "Content-Type: application/json" \
     -d '{"text": "Hello world"}'
   ```

### 2. Using Rate Limiting

1. **Check Rate Limit Status**:
   ```bash
   curl -X GET "http://localhost:8080/api/ai/chat/rate-limit" \
     -H "X-User-ID: user123"
   ```

2. **Handle Rate Limit Responses**:
   ```javascript
   if (response.status === 429) {
     const retryAfter = response.headers.get('Retry-After');
     console.log(`Rate limited. Retry after ${retryAfter} seconds`);
   }
   ```

### 3. Security Best Practices

1. **Input Validation**:
   - Always validate user input before processing
   - Use appropriate input types for validation
   - Sanitize output before sending to users

2. **Rate Limiting**:
   - Monitor rate limit headers
   - Implement exponential backoff for retries
   - Cache responses when possible

3. **Error Handling**:
   - Handle security exceptions gracefully
   - Log security events for monitoring
   - Provide user-friendly error messages

## 📊 Monitoring & Analytics

### Security Events Logging

The system automatically logs security events:
- Rate limit violations
- Malicious input detection
- Translation failures
- Security exceptions

### Performance Metrics

Monitor these metrics:
- Translation cache hit rate
- Rate limit violation frequency
- Language detection accuracy
- API response times

## 🔧 Troubleshooting

### Common Issues

1. **Translation Not Working**:
   - Check Google Translate API key
   - Verify API quota limits
   - Check network connectivity

2. **Rate Limiting Too Strict**:
   - Adjust rate limit configuration
   - Implement caching strategies
   - Consider user tiering

3. **Language Detection Inaccurate**:
   - Review detection patterns
   - Add more language-specific keywords
   - Consider using external language detection APIs

### Debug Mode

Enable debug logging:
```properties
logging.level.com.group02.openevent.ai=DEBUG
```

## 🎯 Future Enhancements

### Planned Features

1. **Advanced Language Detection**:
   - Machine learning-based detection
   - Context-aware language switching
   - Regional dialect support

2. **Enhanced Security**:
   - Behavioral analysis
   - Anomaly detection
   - Advanced threat protection

3. **Performance Optimization**:
   - Distributed caching
   - Async processing
   - Load balancing

## 📝 API Reference

### Response Formats

#### Language Detection Response
```json
{
  "language": "vi",
  "languageName": "Vietnamese",
  "nativeName": "Tiếng Việt",
  "confidence": 0.95,
  "isMixedLanguage": false
}
```

#### Translation Response
```json
{
  "originalText": "Hello world",
  "translatedText": "Xin chào thế giới",
  "sourceLanguage": "en",
  "targetLanguage": "vi",
  "sourceLanguageName": "English",
  "targetLanguageName": "Vietnamese"
}
```

#### Rate Limit Response
```json
{
  "AI_CHAT": {
    "maxRequests": 20,
    "remainingRequests": 15,
    "resetTime": "2024-01-01T12:01:00",
    "isLimitExceeded": false
  }
}
```

#### Error Response
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "rateLimitInfo": {
    "maxRequests": 20,
    "remainingRequests": 0,
    "resetTime": "2024-01-01T12:01:00"
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

## 🏁 Conclusion

The multi-language support and security implementation provides:

✅ **Multi-language Support**:
- Automatic language detection
- Real-time translation
- 8 supported languages
- Caching for performance

✅ **Security Measures**:
- Rate limiting with configurable limits
- Input validation and sanitization
- Malicious content detection
- Security event logging

✅ **Production Ready**:
- Comprehensive error handling
- Monitoring and analytics
- Configurable settings
- API documentation

This implementation significantly enhances the OpenEvent AI system's usability and security, making it suitable for international users while protecting against common security threats.

