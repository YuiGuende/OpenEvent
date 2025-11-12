# Decision Table Testing - Enhanced Chat API

## Feature được kiểm thử

### Tên feature/nghiệp vụ:
**Enhanced Chat API** – Xử lý hội thoại AI đa ngôn ngữ với rate‑limit, kiểm duyệt input, dịch tự động, khôi phục phiên.

### Mã/Link nguồn yêu cầu (BRD/SRS/User Story/AC/Jira):
- Controller: `src/main/java/com/group02/openevent/ai/controller/EnhancedAIController.java`
- Security: `ai/security/AISecurityService.java`, `ai/security/RateLimitingService.java`
- Service: `services/ai/ChatSessionService.java`, `ai/service/LanguageDetectionService.java`, `ai/service/TranslationService.java`
- Frontend: `src/main/resources/static/js/chatbot.js` (health check, retry, signals)
- Endpoint chính: `POST /api/ai/chat/enhanced`

---

## Luật nghiệp vụ (Business Rules)

- BR-01: IF rate limit exceeded (per minute per user) THEN return 429 với thông báo “Rate limit exceeded…”.
- BR-02: IF input invalid (null/empty/malicious/exceed length) THEN 400 với “❌ <error>” (server) hoặc UI hiển thị cảnh báo.
- BR-03: IF `language=auto` THEN detect language từ message.
- BR-04: IF user language ≠ VI THEN translate user input → VI để xử lý; response từ VI → user language.
- BR-05: Process chat via `ChatSessionService.chat()` với `userId`, `sessionId`, `message` đã sanitize/translate.
- BR-06: Validate AI response; IF response invalid THEN thay bằng “❌ <error>”.
- BR-07: IF response chứa tín hiệu `__REDIRECT:...__` THEN xóa tín hiệu và hướng dẫn redirect. IF chứa `__RELOAD__` THEN xóa và reload.
- BR-08: Health: `GET /health` trả `status=UP` + metadata; FE đặt trạng thái Online/Offline.
- BR-09: FE retry tối đa 2 lần khi lỗi mạng/5xx; hiển thị nút “Thử lại” nếu hết lượt.
- BR-10: FE map lỗi HTTP → thông điệp thân thiện: 400/401/403/429/500/503.

Ngưỡng/bao hàm:
- Rate limit phút: AI_CHAT = 20 req/min, TRANSLATION = 50 req/min (server time, LocalDateTime).
- Độ dài: MESSAGE ≤ 2000 chars; GENERAL ≤ 5000 (VNĐ không liên quan).
- Timezone: server local (mặc định), language codes theo enum `Language`.

---

## Điều kiện (Conditions) – Cx

- C1 (Boolean): Rate limit allow? (per userId, type=AI_CHAT). EC: true/false.
- C2 (string): `req.message` sau trim rỗng? EC: empty / non-empty. Boundary: "", " ".
- C3 (enum): Input validation result (AISecurityService). EC: valid / invalid(malicious/length/format).
- C4 (enum): `language` param. EC: "auto" / specific (`vi`, `en`, ...). Domain: ISO code defined in `Language`.
- C5 (Boolean): Translation services available? EC: available/unavailable (nếu unavailable, fallback trả tiếng Việt).
- C6 (Boolean): Session valid? EC: sessionId present non-empty. Boundary: null, "".
- C7 (HTTP): Backend chat result OK? EC: 2xx / 4xx / 5xx.
- C8 (flags): Response contains signals? EC: none / `__REDIRECT:...__` / `__RELOAD__`.
- C9 (Boolean): Network/Fetch error on FE? EC: error/no error.
- C10 (Boolean): AI response passes security validation? EC: pass/fail.

Miền giá trị & điểm biên đã nêu theo từng điều kiện (trim, length, null/empty, rate window phút).

---

## Hành động/Kết quả (Actions) – Ax

- A1: Trả 429 (server) với message rate-limit; FE hiển thị lỗi 429.
- A2: Trả 400 (server) với “❌ <error>”; FE map 400 → “Dữ liệu không hợp lệ…”.
- A3: Detect language (server) khi `language=auto`.
- A4: Translate input → VI trước khi chat; Translate output → user language (nếu cần).
- A5: Gọi `ChatSessionService.chat()` với `ChatRequest(messageVI, userId, sessionId)`.
- A6: Validate AI response; nếu fail → thay nội dung cảnh báo (server).
- A7: FE remove signals, hiển thị msg sạch; nếu `REDIRECT` → hiển thị countdown và `window.location.href`; nếu `RELOAD` → `location.reload()`.
- A8: FE health check `/health` → set Online/Offline.
- A9: FE retry tự động tối đa 2 lần; nếu hết → hiện nút “Thử lại” để user gọi lại.
- A10: FE map mã lỗi: 401/403/429/500/503 thành chuỗi tiếng Việt phù hợp.

Quan hệ/sequence: A3→A4→A5→A6→A7 là chuỗi xử lý 1 request thành công. A1/A2/A8/A9/A10 là nhánh lỗi/ngoại lệ.

---

## Ưu tiên xung đột (Precedence/Priority)

1) Rate limit (C1=false) → A1.
2) Input invalid (C3=invalid OR C2=empty) → A2.
3) Session invalid (C6=false) → FE thông báo lỗi phiên (map 400) trước khi gọi server.
4) Xử lý thường: A3→A4→A5→A6→A7.
5) FE error/HTTP error (C7 non-2xx hoặc C9=true) → A9/A10.

---

## Tổ hợp không khả thi (Infeasible Combos)

- IC-01: C1=false (rate limit) cùng lúc C7=2xx – bị chặn trước, không gọi chat.
- IC-02: C2=empty nhưng C3=valid – input rỗng luôn invalid.
- IC-03: C6=false (sessionId rỗng) nhưng A5 được gọi – FE chặn trước khi gọi API.
- IC-04: C8 có signal khi C7 non-2xx – không có body hợp lệ để chứa signal.

---

## Fallback/Default

Nếu không khớp rule rõ ràng: FE hiển thị “❌ Lỗi không xác định. Vui lòng thử lại.” và ghi log; server trả 5xx được map thành “Lỗi máy chủ”.

---

## Ràng buộc kỹ thuật & bối cảnh test

- Role: người dùng đã đăng nhập (backend gọi `SessionUtils.requireUser()`); rate-limit theo userId.
- Preconditions: USER_ID hợp lệ, `sessionId` hiện có/khởi tạo; API base URL cấu hình đúng; translation service “available” để test nhánh dịch.
- Postconditions: Lịch sử chat được lưu/khôi phục (FE `sessionStorage`), tín hiệu được xử lý, trạng thái kết nối cập nhật.
- API liên quan:
  - `POST /api/ai/chat/enhanced` (body: `{message, userId, sessionId}`)
  - `GET /api/ai/chat/enhanced/health`
  - `GET /api/ai/chat/enhanced/rate-limit`
  - (phụ) `/sessions`, `/history`, `/translate`, `/detect-language`
- Mã lỗi FE map: 400, 401, 403, 429, 500, 503.
- Rate window: 1 phút (per user & type). Timezone: server local.

---

## Ví dụ cụ thể

1) Success – auto detect + translate
   - Input: language=auto, message="Hello", userId valid, sessionId valid.
   - Expected: detect EN → translate to VI → chat → translate back EN → 200 với message EN.

2) Rate limit exceeded
   - Input: user gửi >20 req trong 1 phút.
   - Expected: 429 từ server, FE hiển thị “Bạn đã gửi quá nhiều tin nhắn…”.

3) Input invalid (malicious)
   - Input: message chứa `<script>alert(1)</script>`.
   - Expected: 400 “Input contains potentially malicious content”.

4) Session invalid (FE)
   - Input: sessionId rỗng trên FE.
   - Expected: FE không gọi API, hiển thị “⚠️ Lỗi phiên làm việc…”.

5) HTTP 503 + retry
   - Input: server tạm thời 503.
   - Expected: FE retry tối đa 2 lần; nếu còn lỗi → nút “Thử lại”.

6) Signal redirect
   - Input: reply chứa “__REDIRECT:/events__ Tới trang sự kiện”.
   - Expected: FE hiển thị message sạch, rồi redirect sau ~1.5s.

---

## Giới hạn & giả định

- Giả định: người dùng đã đăng nhập (backend yêu cầu session); dịch vụ dịch khả dụng phần lớn thời gian; response dạng text có thể chứa signal.
- Không phạm vi: lưu lịch sử trên server; bảo đảm idempotency ở FE; kiểm soát chính sách content ngoài các pattern sẵn có.

---

## Tài liệu đính kèm

- Code tham chiếu: `EnhancedAIController.chat()` (L129–193), `checkApiHealth()` và `sendMessageToApi()` trong `static/js/chatbot.js`, `RateLimitingService`, `AISecurityService`.
- UI: `templates/fragments/chatbot.html` (khung FE).

---

## Checklist xác nhận

- [x] Điều kiện có miền giá trị & điểm biên.
- [x] Hành động đơn trị, rõ ràng.
- [x] Có thứ tự ưu tiên khi xung đột.
- [x] Liệt kê tổ hợp không khả thi.
- [x] Có ví dụ thực tế kèm dữ liệu.


---

## Mapping Decision Table → Unit tests (hiện có/đề xuất)

- BR-01 (Rate limit exceeded → 429)
  - ĐÃ CÓ: `chat_rateLimit(false → 429)`, `sessions_rateLimit(false → 429)`, `createSession_rateLimitDenied_429`, `history_rateLimitDenied_429`, `enhancedTranslate_rateLimitDenied_429`, `detectLanguage_rateLimitDenied_429` trong `EnhancedAIControllerTest`.
- BR-02 (Input invalid → 400)
  - ĐÃ CÓ: `detectLanguage_blank_400` (text rỗng).
  - ĐỀ XUẤT THÊM: `chat_inputInvalid_400` (message rỗng/malicious/over-length) để bao phủ rule này cho endpoint chat.
- BR-03 (language=auto → detect)
  - ĐÃ CÓ: `chat_translations` với `lang=auto` (mock detect).
- BR-04 (Translate 2 chiều)
  - ĐÃ CÓ: `chat_translations` với `lang=en/vi` (translate input/output).
- BR-05 (Process chat thành công)
  - ĐÃ CÓ: `chat_translations` happy-path; `createSession_languageFlow`, `history_languageFlow`.
- BR-06 (Validate AI response)
  - ĐÃ CÓ: `chat_aiResponseValidation(true/false)` – thay nội dung khi response invalid.
- BR-07 (Signals `__REDIRECT__/__RELOAD__` – FE xử lý)
  - CHƯA TEST UNIT (nằm ở FE). ĐỀ XUẤT: FE test (Jest/Playwright) hoặc integration test mock response chứa signal để assert FE hành vi.
- BR-08 (Health endpoint)
  - ĐÃ CÓ: `simple_get_endpoints_ok` bao gồm `/health`.
- BR-09 (FE retry khi 5xx/network)
  - CHƯA TEST UNIT (FE). ĐỀ XUẤT: FE test cho retry và nút “Thử lại”.
- BR-10 (FE error mapping 400/401/403/429/500/503)
  - CHƯA TEST UNIT (FE). ĐỀ XUẤT: FE test cho mapping thông điệp.

Vị trí test class:

```1:20:src/test/java/com/group02/openevent/ai/controller/EnhancedAIControllerTest.java
package com.group02.openevent.ai.controller;

@ActiveProfiles("test")
class EnhancedAIControllerTest {
    // ... các phương thức test được liệt kê ở trên ...
}
```

Gợi ý bổ sung test còn thiếu (mô tả ngắn):
- `chat_inputInvalid_400`: gửi `message=""` và `message="<script>..."` → expect 400, body chứa “❌”.
- FE tests (nếu phạm vi cho phép): mock API trả `__REDIRECT:/x__`/`__RELOAD__`, 503, 401/403/429 để assert hành vi retry/mapping/redirect.


