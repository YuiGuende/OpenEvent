# 🔄 LibreTranslate Migration Summary

## 📋 Tổng quan

Đã hoàn thành việc migration từ Google Translate API sang LibreTranslate cho dự án OpenEvent AI.

## ✨ Những gì đã thực hiện

### 1. 🆕 Tạo LibreTranslateService
- **File**: `src/main/java/com/group02/openevent/ai/service/LibreTranslateService.java`
- **Tính năng**:
  - Hỗ trợ public instance và self-hosted
  - Fallback URLs tự động
  - Caching tích hợp
  - Error handling tốt
  - Async support

### 2. 🔄 Cập nhật TranslationService
- **File**: `src/main/java/com/group02/openevent/ai/service/TranslationService.java`
- **Thay đổi**:
  - Loại bỏ Google Translate API
  - Sử dụng LibreTranslateService
  - Giữ nguyên interface public
  - Thêm methods mới

### 3. 🎮 Tạo TranslationController
- **File**: `src/main/java/com/group02/openevent/ai/controller/TranslationController.java`
- **Endpoints**:
  - `POST /api/ai/translation/translate` - Dịch text
  - `POST /api/ai/translation/translate-async` - Dịch async
  - `GET /api/ai/translation/languages` - Lấy danh sách ngôn ngữ
  - `GET /api/ai/translation/status` - Kiểm tra trạng thái
  - `GET /api/ai/translation/test` - Test kết nối
  - `POST /api/ai/translation/clear-cache` - Xóa cache

### 4. ⚙️ Cập nhật Configuration
- **File**: `src/main/resources/application.properties`
- **Thêm**:
  ```properties
  # LibreTranslate Configuration
  libretranslate.api.url=${LIBRETRANSLATE_API_URL:https://libretranslate.com}
  libretranslate.api.key=${LIBRETRANSLATE_API_KEY:}
  libretranslate.fallback.urls=https://translate.argosopentech.com,https://libretranslate.de
  ```

### 5. 🐳 Docker Support
- **File**: `docker-compose-libretranslate.yml`
- **File**: `nginx.conf`
- **Tính năng**:
  - Self-hosted LibreTranslate
  - Nginx reverse proxy
  - Rate limiting
  - Health checks
  - Resource limits

### 6. 🧪 Test Interface
- **File**: `src/main/resources/static/libretranslate-test.html`
- **Tính năng**:
  - Test translation
  - Test async translation
  - Check service status
  - Load supported languages
  - Beautiful UI

### 7. 📚 Documentation
- **File**: `LIBRETRANSLATE_SETUP.md`
- **File**: `LIBRETRANSLATE_MIGRATION.md`
- **Nội dung**:
  - Hướng dẫn setup
  - Troubleshooting
  - Performance tips
  - Security notes

## 🚀 Cách sử dụng

### 1. Sử dụng Public Instance (Đơn giản nhất)
```bash
# Không cần cấu hình gì
# Chỉ cần restart application
```

### 2. Self-hosted với Docker
```bash
# Chạy LibreTranslate
docker-compose -f docker-compose-libretranslate.yml up -d

# Cấu hình application.properties
libretranslate.api.url=http://localhost:5000
```

### 3. Test Integration
```bash
# Mở browser
http://localhost:8080/libretranslate-test.html

# Hoặc test API trực tiếp
curl -X POST "http://localhost:8080/api/ai/translation/translate" \
  -d "text=Hello&sourceLang=en&targetLang=vi"
```

## 🔧 API Usage

### Translation
```java
@Autowired
private TranslationService translationService;

// Sync translation
String result = translationService.translate("Hello", Language.ENGLISH, Language.VIETNAMESE);

// Async translation
CompletableFuture<String> future = translationService.translateAsync("Hello", Language.ENGLISH, Language.VIETNAMESE);
```

### REST API
```bash
# Translate text
curl -X POST "http://localhost:8080/api/ai/translation/translate" \
  -d "text=Hello&sourceLang=en&targetLang=vi"

# Get supported languages
curl "http://localhost:8080/api/ai/translation/languages"

# Check status
curl "http://localhost:8080/api/ai/translation/status"
```

## 📊 So sánh với Google Translate

| Tính năng | Google Translate | LibreTranslate |
|-----------|------------------|----------------|
| **Chi phí** | ❌ Có phí | ✅ Miễn phí |
| **Chất lượng** | ✅ Tốt | ⚠️ Trung bình |
| **Tốc độ** | ✅ Nhanh | ⚠️ Chậm hơn |
| **Độ ổn định** | ✅ Cao | ⚠️ Trung bình |
| **Tự host** | ❌ Không | ✅ Có |
| **API Key** | ❌ Cần | ✅ Không cần |
| **Rate Limit** | ✅ Cao | ⚠️ Thấp |
| **Ngôn ngữ** | ✅ 100+ | ✅ 100+ |

## 🎯 Lợi ích

### ✅ Ưu điểm
- **Miễn phí**: Không cần API key
- **Tự host**: Kiểm soát hoàn toàn
- **Mã nguồn mở**: Có thể tùy chỉnh
- **Dễ tích hợp**: REST API đơn giản
- **Fallback**: Tự động chuyển đổi instance

### ⚠️ Nhược điểm
- **Chất lượng**: Kém hơn Google Translate
- **Tốc độ**: Chậm hơn
- **Tài nguyên**: Cần nhiều RAM/CPU khi self-host
- **Rate limit**: Public instance có giới hạn

## 🔄 Migration Steps

### 1. Backup Configuration
```bash
# Backup current config
cp application.properties application.properties.backup
```

### 2. Update Configuration
```properties
# Comment out Google Translate
# google.translate.api.key=${GOOGLE_TRANSLATE_API_KEY}

# Add LibreTranslate
libretranslate.api.url=${LIBRETRANSLATE_API_URL:https://libretranslate.com}
libretranslate.api.key=${LIBRETRANSLATE_API_KEY:}
```

### 3. Restart Application
```bash
# Restart Spring Boot application
./mvnw spring-boot:run
```

### 4. Test Integration
```bash
# Test translation
curl -X POST "http://localhost:8080/api/ai/translation/translate" \
  -d "text=Hello&sourceLang=en&targetLang=vi"

# Check status
curl "http://localhost:8080/api/ai/translation/status"
```

## 🚨 Troubleshooting

### 1. Service không khởi động
```bash
# Check logs
docker logs openevent-libretranslate

# Check port
netstat -tulpn | grep 5000

# Restart
docker-compose -f docker-compose-libretranslate.yml restart
```

### 2. Translation fails
```bash
# Check API
curl http://localhost:5000/languages

# Check rate limits
curl -v http://localhost:5000/translate

# Check memory
docker stats openevent-libretranslate
```

### 3. Performance issues
```bash
# Increase resources
# Edit docker-compose-libretranslate.yml
# Increase memory and CPU limits
```

## 📈 Performance Tips

### 1. Caching
```java
// Cache đã được tích hợp
// Có thể điều chỉnh cache size
ai.multilang.translation-cache-size=1000
```

### 2. Async Processing
```java
// Sử dụng async cho batch processing
CompletableFuture<String> future = translationService.translateAsync(text, source, target);
```

### 3. Fallback URLs
```java
// Tự động fallback sang các URL khác
// Nếu primary instance fail
```

## 🔐 Security

### 1. API Keys (Tùy chọn)
```bash
# Enable API keys
LT_API_KEYS=true
```

### 2. Rate Limiting
```bash
# Set limits
LT_REQ_LIMIT=1000
LT_BATCH_LIMIT=32
LT_CHAR_LIMIT=5000
```

### 3. Network Security
```yaml
# Chỉ expose localhost
ports:
  - "127.0.0.1:5000:5000"
```

## 🎉 Kết luận

Migration đã hoàn thành thành công! LibreTranslate cung cấp:

- ✅ **Giải pháp miễn phí** thay thế Google Translate
- ✅ **Tích hợp dễ dàng** với Spring Boot
- ✅ **Tự host** để kiểm soát hoàn toàn
- ✅ **Fallback** tự động khi có lỗi
- ✅ **Caching** để tối ưu performance
- ✅ **API đầy đủ** cho testing và monitoring

Chỉ cần cập nhật configuration và restart application là có thể sử dụng ngay! 🚀

## 📞 Support

Nếu có vấn đề gì, hãy kiểm tra:
1. **Logs**: `docker logs openevent-libretranslate`
2. **Status**: `curl http://localhost:8080/api/ai/translation/status`
3. **Test**: `http://localhost:8080/libretranslate-test.html`
4. **Documentation**: `LIBRETRANSLATE_SETUP.md`





