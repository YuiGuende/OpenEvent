# Chat API Endpoints Test

## Test the 4 main endpoints:

### 1. Create Session
```bash
curl -X POST http://localhost:8080/api/ai/sessions \
  -H "Content-Type: application/json" \
  -d '{"userId": 2, "title": "Phiên A"}'
```

Expected: Returns sessionId (Long)

### 2. List Sessions
```bash
curl -X GET http://localhost:8080/api/ai/sessions/2
```

Expected: Returns list of sessions for user 2

### 3. Get History
```bash
curl -X GET "http://localhost:8080/api/ai/sessions/2/history?sessionId=<sessionId>"
```

Expected: Returns empty array initially

### 4. Send Chat Message
```bash
curl -X POST http://localhost:8080/api/ai/sessions/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello", "userId": 2, "sessionId": <sessionId>}'
```

Expected: Returns AI reply

### 5. Get History Again
```bash
curl -X GET "http://localhost:8080/api/ai/sessions/2/history?sessionId=<sessionId>"
```

Expected: Returns 2 messages (user + AI)

## Success Criteria:
- ✅ New tables chat_session & chat_message (IDs Long/IDENTITY)
- ✅ 4 endpoints work: list sessions, create session, history by sessionId, chat with sessionId
- ✅ Messages are scoped by sessionId; updatedAt of session is touched on every message
- ✅ Authorization ensures user can access only own sessions/messages (TODO: implement Principal mapping)
- ✅ Build passes and app starts
