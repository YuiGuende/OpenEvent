# Event Volunteer-Host Realtime Chat Design

## Existing Building Blocks

- `Role`, `User`, `Customer`, `Host`, `VolunteerApplication`, `VolunteerStatus`, `Event`: core domain already models hosts, customers, and approved volunteers tied to events.
- `VolunteerServiceImpl` exposes helpers such as `isCustomerApprovedVolunteer` and `hasCustomerAppliedAsVolunteer` to validate chat eligibility per event.
- `AuthServiceImpl` + `UserServiceImpl` populate HTTP session attributes (`USER_ID`, `ACCOUNT_ID`, `USER_ROLE`) that can be used to authorize chat participants.
- `WebSocketConfig` and `WebSocketUtil` provide a working SockJS/STOMP setup (`/ws`, `/app`, `/topic`, `/queue`) already used by realtime notifications.
- `Notification` front-end in `fragments/header.html` shows how the app initializes SockJS and subscribes to user-specific queues, which can be mirrored for chat.

## Proposed Architecture

1. **Database Layer**  
   Introduce two tables/entities:
   - `event_chat_room` links an event, its host user, and an approved volunteer. Enforce uniqueness per (event, host_user_id, volunteer_user_id).
   - `event_chat_message` stores messages associated with a chat room, capturing sender user, sender role, body, and timestamps.

2. **JPA Layer**  
   Add `EventChatRoom` and `EventChatMessage` entities plus enums/DTOs. Provide repositories (`IEventChatRoomRepo`, `IEventChatMessageRepo`) with helpers to locate rooms and fetch message history.

3. **Service Layer**  
   Create `EventChatService` that:
   - Validates the current session user (via `UserService.getCurrentUser`).
   - Checks host/volunteer permissions (`VolunteerServiceImpl`, `Event.getHost().getUser()`).
   - Finds or creates the corresponding room.
   - Persists a new message and returns a DTO for broadcasting.
   - Uses `SimpMessagingTemplate` (or a dedicated component) to publish to both participants.

4. **WebSocket Controller**  
   Implement `EventChatWebSocketController` with `@MessageMapping("/event-chat.send")`. It delegates to the service, then calls `convertAndSendToUser` for host and volunteer destinations like `/queue/event-chat/{roomId}`. 

5. **REST API Endpoints**  
   Add `EventChatController` under `/api/event-chat`:
   - `GET /rooms/{eventId}` → list rooms visible to the current user.
   - `GET /rooms/{roomId}/messages` → fetch paginated history.
   - Optional `POST /rooms/{eventId}` to pre-create room after approval.

6. **Front-End Integration**  
   - Introduce JS (`static/js/event-chat.js`) that mirrors notification setup: initialize SockJS, subscribe to `/user/queue/event-chat`, render messages, and send via STOMP.
   - Build host/volunteer UI fragments (e.g. `templates/event/chat-room.html`) showing room list and message pane.
   - Use REST APIs to hydrate initial state; push live updates via WebSocket.

7. **Access Control**  
   - Extend `SessionInterceptor` or add new interceptor to guard `/api/event-chat/**` and `/app/event-chat.*` paths.
   - Ensure only the event host or approved volunteer can join/send.

## Implementation Checklist

- [ ] Flyway/Liquibase migration for `event_chat_room` & `event_chat_message` tables.
- [ ] Entities, enums (e.g. `ChatSenderRole`), repositories.
- [ ] `EventChatService` + helper components for authorization and room/message management.
- [ ] WebSocket controller (`@MessageMapping`) and DTOs.
- [ ] REST controller for rooms, history, and optional room creation.
- [ ] Front-end JS & templates for host/volunteer chat UI.
- [ ] Security/interceptor updates ensuring authenticated access and role checks.
- [ ] Unit/integration tests covering service logic and WebSocket flow.

## Testing Notes

- **Unit Tests**: mock repositories to verify volunteer/host authorization, room creation, message persistence, and DTO mapping.
- **Integration Tests**: use Spring `WebSocketStompClient` to simulate host & volunteer sessions exchanging messages.
- **Manual QA**: login as host and approved volunteer in separate browsers; verify real-time updates, history load, and permission enforcement.
- **Regression**: confirm existing WebSocket notifications and session logic remain unaffected.
