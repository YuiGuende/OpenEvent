# 🌍 LibreTranslate Integration Guide

## 📋 Tổng quan

LibreTranslate là một dịch vụ dịch thuật mã nguồn mở, miễn phí thay thế cho Google Translate API. Nó hỗ trợ nhiều ngôn ngữ và có thể được host riêng hoặc sử dụng public instance.

## ✨ Ưu điểm

- ✅ **Miễn phí**: Không cần API key hoặc thanh toán
- ✅ **Mã nguồn mở**: Có thể tự host và tùy chỉnh
- ✅ **REST API đơn giản**: Dễ tích hợp
- ✅ **Nhiều ngôn ngữ**: Hỗ trợ 100+ ngôn ngữ
- ✅ **Tự host**: Kiểm soát hoàn toàn dữ liệu

## ⚠️ Nhược điểm

- ❌ **Chất lượng**: Kém hơn Google Translate
- ❌ **Tốc độ**: Chậm hơn Google Translate
- ❌ **Độ ổn định**: Public instance có thể bị rate-limit
- ❌ **Tài nguyên**: Cần nhiều RAM/CPU khi tự host

## 🚀 Cách sử dụng

### 1. Sử dụng Public Instance (Đơn giản nhất)

```bash
# Không cần cấu hình gì, sử dụng ngay
curl -X POST "https://libretranslate.com/translate" \
  -H "Content-Type: application/json" \
  -d '{
    "q": "Xin chào",
    "source": "vi",
    "target": "en",
    "format": "text"
  }'
```

**Cấu hình application.properties:**
```properties
libretranslate.api.url=https://libretranslate.com
libretranslate.api.key=
```

### 2. Tự Host với Docker (Khuyến nghị)

#### Bước 1: Tạo file docker-compose.yml
```yaml
version: '3.8'
services:
  libretranslate:
    image: libretranslate/libretranslate:latest
    container_name: openevent-libretranslate
    ports:
      - "5000:5000"
    environment:
      - LT_API_KEYS=true
      - LT_REQ_LIMIT=1000
      - LT_CHAR_LIMIT=5000
    volumes:
      - libretranslate_db:/app/db
    restart: unless-stopped

volumes:
  libretranslate_db:
```

#### Bước 2: Chạy LibreTranslate
```bash
# Sử dụng file docker-compose có sẵn
docker-compose -f docker-compose-libretranslate.yml up -d

# Hoặc chạy trực tiếp
docker run -d -p 5000:5000 libretranslate/libretranslate:latest
```

#### Bước 3: Cấu hình application.properties
```properties
libretranslate.api.url=http://localhost:5000
libretranslate.api.key=your-api-key-if-needed
```

### 3. Sử dụng file Docker Compose có sẵn

```bash
# Clone project và chạy
git clone <your-repo>
cd OpenEvent

# Chạy LibreTranslate
docker-compose -f docker-compose-libretranslate.yml up -d

# Kiểm tra trạng thái
docker-compose -f docker-compose-libretranslate.yml ps

# Xem logs
docker-compose -f docker-compose-libretranslate.yml logs -f
```

## 🔧 Cấu hình nâng cao

### Environment Variables

```bash
# API Keys (tùy chọn)
LT_API_KEYS=true
LT_API_KEYS_DB_PATH=/app/db/api_keys.db

# Giới hạn request
LT_REQ_LIMIT=1000          # Requests per hour
LT_BATCH_LIMIT=32          # Batch size
LT_CHAR_LIMIT=5000         # Character limit per request

# Ngôn ngữ
LT_LOAD_ONLY=vi,en,zh,ja,ko,fr,de,es,it,pt,ru,ar,th,id,ms
LT_DEFAULT_SRC_LANG=vi
LT_DEFAULT_TGT_LANG=en

# GPU Support
LT_GPU=false

# Web UI
LT_DISABLE_WEB_UI=false
LT_DISABLE_API=false
```

### Nginx Reverse Proxy (Tùy chọn)

```bash
# Sử dụng nginx.conf có sẵn
docker-compose -f docker-compose-libretranslate.yml up -d

# Truy cập qua nginx
curl http://localhost:8080/translate
```

## 🧪 Testing

### 1. Test API Endpoints

```bash
# Test translation
curl -X POST "http://localhost:5000/translate" \
  -H "Content-Type: application/json" \
  -d '{
    "q": "Xin chào",
    "source": "vi",
    "target": "en",
    "format": "text"
  }'

# Test languages
curl "http://localhost:5000/languages"

# Test health
curl "http://localhost:5000/languages"
```

### 2. Test Spring Boot Integration

```bash
# Test translation endpoint
curl -X POST "http://localhost:8080/api/ai/translation/translate" \
  -d "text=Xin chào&sourceLang=vi&targetLang=en"

# Test status
curl "http://localhost:8080/api/ai/translation/status"

# Test languages
curl "http://localhost:8080/api/ai/translation/languages"
```

### 3. Test trong Code

```java
@Autowired
private TranslationService translationService;

// Test translation
String result = translationService.translate("Hello", Language.ENGLISH, Language.VIETNAMESE);
System.out.println(result); // "Xin chào"

// Test connection
boolean isAvailable = translationService.testConnection();
System.out.println("Service available: " + isAvailable);
```

## 📊 Monitoring

### 1. Health Checks

```bash
# Check container status
docker ps | grep libretranslate

# Check logs
docker logs openevent-libretranslate

# Check API health
curl http://localhost:5000/languages
```

### 2. Metrics

```bash
# Check cache size
curl "http://localhost:8080/api/ai/translation/status"

# Clear cache
curl -X POST "http://localhost:8080/api/ai/translation/clear-cache"
```

## 🔄 Migration từ Google Translate

### 1. Cập nhật Dependencies

```xml
<!-- Không cần thay đổi dependencies -->
<!-- LibreTranslate sử dụng HTTP client có sẵn -->
```

### 2. Cập nhật Configuration

```properties
# Cũ (Google Translate)
# google.translate.api.key=${GOOGLE_TRANSLATE_API_KEY}

# Mới (LibreTranslate)
libretranslate.api.url=${LIBRETRANSLATE_API_URL:https://libretranslate.com}
libretranslate.api.key=${LIBRETRANSLATE_API_KEY:}
```

### 3. Cập nhật Code

```java
// Code không cần thay đổi
// TranslationService đã được cập nhật để sử dụng LibreTranslate
String result = translationService.translate(text, sourceLang, targetLang);
```

## 🚨 Troubleshooting

### 1. LibreTranslate không khởi động

```bash
# Check logs
docker logs openevent-libretranslate

# Check port conflicts
netstat -tulpn | grep 5000

# Restart container
docker-compose -f docker-compose-libretranslate.yml restart
```

### 2. Translation fails

```bash
# Check API availability
curl http://localhost:5000/languages

# Check rate limits
curl -v http://localhost:5000/translate

# Check memory usage
docker stats openevent-libretranslate
```

### 3. Performance issues

```bash
# Increase memory limit
docker-compose -f docker-compose-libretranslate.yml up -d --scale libretranslate=2

# Check resource usage
docker stats

# Optimize configuration
# Giảm LT_REQ_LIMIT nếu cần
```

## 📈 Performance Tips

### 1. Caching

```java
// TranslationService đã có cache built-in
// Cache size có thể điều chỉnh trong application.properties
ai.multilang.translation-cache-size=1000
```

### 2. Batch Processing

```java
// Sử dụng async translation cho batch
CompletableFuture<String> future = translationService.translateAsync(text, source, target);
```

### 3. Fallback URLs

```java
// LibreTranslateService tự động fallback sang các URL khác
// Nếu primary instance fail, sẽ thử các fallback URLs
```

## 🔐 Security

### 1. API Keys (Tùy chọn)

```bash
# Enable API keys
LT_API_KEYS=true

# Set API key
LT_API_KEYS_DB_PATH=/app/db/api_keys.db
```

### 2. Rate Limiting

```bash
# Set request limits
LT_REQ_LIMIT=1000
LT_BATCH_LIMIT=32
LT_CHAR_LIMIT=5000
```

### 3. Network Security

```yaml
# Chỉ expose port cần thiết
ports:
  - "127.0.0.1:5000:5000"  # Chỉ localhost
```

## 📝 Notes

1. **Public Instance**: Có thể bị rate-limit, không ổn định
2. **Self-hosted**: Cần nhiều RAM (2GB+), CPU (1 core+)
3. **Quality**: Kém hơn Google Translate nhưng đủ dùng
4. **Languages**: Hỗ trợ 100+ ngôn ngữ
5. **Updates**: Cập nhật thường xuyên để có model mới

## 🎉 Kết luận

LibreTranslate là lựa chọn tốt để thay thế Google Translate API:
- ✅ Miễn phí và mã nguồn mở
- ✅ Dễ tích hợp và cấu hình
- ✅ Có thể tự host để kiểm soát
- ✅ Hỗ trợ đầy đủ tính năng cần thiết

Chỉ cần cập nhật configuration và restart application là có thể sử dụng ngay! 🚀

