## A. Kết luận ngắn
- EC2 HTTP = 400 Bad Request; mapped by `GlobalExceptionHandler.handleRuntimeException`.
- EC3 Redirect = Yes (controller luôn trả `redirect:/manage/event/{id}/getting-stared` kể cả thiếu field; test `TC04` xác nhận).
- EC4 Call service on invalid date = No (binding `LocalDateTime` lỗi ném `BindException` trước khi vào controller; test `TC09` trả 400).
- Redirect URL = `/manage/event/{id}/getting-stared` (typo “stared”).
- EventType enum = {`MUSIC`, `WORKSHOP`, `COMPETITION`, `FESTIVAL`, `OTHERS`}.
- ES3 fallback = Generic `Event` khi `eventType` null/không khớp.
- Mapper exception type = `RuntimeException`; Controller HTTP = 400 (bị bắt bởi `handleRuntimeException`).
- UP2 = 404 from `EventController.uploadMultipleImages` (controller trả `ResponseEntity.status(HttpStatus.NOT_FOUND)`).
- UP3 = 400 tại controller (kiểm tra độ dài mảng và trả `BAD_REQUEST`).
- UP4 = policy skip = Bỏ qua file rỗng và vẫn trả 200; ảnh rỗng không thêm vào `eventImages`.
- UP5 = HTTP 500; rollback = No (không @Transactional, file đã upload trước lỗi vẫn giữ; DB chưa lưu vì `saveEvent` gọi sau vòng lặp).
- Date format = `LocalDateTime` với `@DateTimeFormat(iso = ISO.DATE_TIME)` và `ObjectMapper` đăng ký `JavaTimeModule`.
- Trim input = No (title/fields map thẳng, không `.trim()`).
- DTO fields = `{id, title, eventType, description, publicDate, enrollDeadline, startsAt, endsAt, status, hostId, organizationId}`.

## B. Bằng chứng
- EC2  
```94:108:src/main/java/com/group02/openevent/controller/event/EventController.java
        EventResponse savedEvent =  eventService.saveEvent(request,hostId);
        return "redirect:/manage/event/" + savedEvent.getId()+ "/getting-stared";
```
```135:145:src/test/java/com/group02/openevent/controller/EventControllerTest.java
        when(eventService.saveEvent(any(EventCreationRequest.class)))
                .thenThrow(new RuntimeException("Save failed"));
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", title)
                        .param("eventType", eventType))
                .andExpect(status().is4xxClientError());
```
```19:38:src/main/java/com/group02/openevent/config/GlobalExceptionHandler.java
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }
```
- EC3  
```94:103:src/main/java/com/group02/openevent/controller/event/EventController.java
    public String createEvent(@ModelAttribute("eventForm") EventCreationRequest request, Model model, HttpSession session) {
        EventResponse savedEvent =  eventService.saveEvent(request,hostId);
        return "redirect:/manage/event/" + savedEvent.getId()+ "/getting-stared";
    }
```
```149:158:src/test/java/com/group02/openevent/controller/EventControllerTest.java
        mockMvc.perform(post("/api/events/saveEvent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/event/101/getting-stared"));
```
- EC4  
```95:103:src/main/java/com/group02/openevent/controller/event/EventController.java
    public String createEvent(@ModelAttribute("eventForm") EventCreationRequest request, Model model, HttpSession session) {
```
```34:43:src/main/java/com/group02/openevent/dto/request/create/EventCreationRequest.java
    LocalDateTime startsAt;
    LocalDateTime endsAt;
```
```251:256:src/test/java/com/group02/openevent/controller/EventControllerTest.java
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", "Bad Date")
                        .param("eventType", "WORKSHOP")
                        .param("startsAt", "invalid-date"))
                .andExpect(status().isBadRequest());
```
- Redirect URL  
```100:103:src/main/java/com/group02/openevent/controller/event/EventController.java
        return "redirect:/manage/event/" + savedEvent.getId()+ "/getting-stared";
```
- EventType enum  
```6:8:src/main/java/com/group02/openevent/model/enums/EventType.java
public enum EventType {
    MUSIC, WORKSHOP, COMPETITION, FESTIVAL, OTHERS
}
```
- ES3 & subtype mapping  
```71:99:src/main/java/com/group02/openevent/service/impl/EventServiceImpl.java
        if (type == null) {
            event = new Event();
        }else {
            switch (request.getEventType()) {
                case WORKSHOP -> event = new WorkshopEvent();
                case MUSIC -> event = new MusicEvent();
                case FESTIVAL -> event = new FestivalEvent();
                case COMPETITION -> event = new CompetitionEvent();
                default -> event = new Event();
            }
        }
```
- Mapper exception & HTTP  
```142:146:src/test/java/com/group02/openevent/service/EventServiceTest.java
        doThrow(RuntimeException.class).when(eventMapper).createEventFromRequest(any(), any());
        assertThrows(RuntimeException.class, () -> eventService.saveEvent(req));
```
```19:38:src/main/java/com/group02/openevent/config/GlobalExceptionHandler.java
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest();
    }
```
- UP2 / UP3 / UP4 / UP5  
```211:247:src/main/java/com/group02/openevent/controller/event/EventController.java
            if (!optionalEvent.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Event not found"));
            }
            if (files.length != orderIndexes.length || files.length != mainPosters.length) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid input: arrays have mismatched lengths"));
            }
                if (file.isEmpty()) continue;
                String url = imageService.saveImage(file);
            Event savedEvent = eventService.saveEvent(event);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
```
```18:24:src/main/java/com/group02/openevent/service/impl/CloudinaryServiceImpl.java
    public String saveImage(MultipartFile file) {
        try {
            return cloudinaryUtil.upload(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }
```
```355:358:src/main/java/com/group02/openevent/service/impl/EventServiceImpl.java
    public Event saveEvent(Event event) {
        return eventRepo.save(event);
    }
```
- Date format  
```90:96:src/main/java/com/group02/openevent/model/event/Event.java
    @Column(name = "starts_at", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endsAt;
```
```12:17:src/main/java/com/group02/openevent/config/JacksonConfig.java
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
```
- Trim input  
```169:182:target/generated-sources/annotations/com/group02/openevent/mapper/EventMapperImpl.java
    public void createEventFromRequest(EventCreationRequest request, Event event) {
        if ( request == null ) {
            return;
        }
        event.setTitle( request.getTitle() );
        event.setDescription( request.getDescription() );
    }
```
- DTO fields  
```34:50:src/main/java/com/group02/openevent/dto/request/create/EventCreationRequest.java
    Integer id;
    String title;
    EventType eventType;
    String description;
    LocalDateTime publicDate;
    LocalDateTime enrollDeadline;
    LocalDateTime startsAt;
    LocalDateTime endsAt;
    EventStatus status = EventStatus.DRAFT;
    Long hostId;
    Long organizationId;
```

## C. Patch đề xuất
- Sửa redirect URL về “getting-started” để thống nhất với view/tài liệu; cần cập nhật các test kiểm tra `redirectedUrl("/manage/event/...")`.

```diff
--- a/src/main/java/com/group02/openevent/controller/event/EventController.java
+++ b/src/main/java/com/group02/openevent/controller/event/EventController.java
@@
-        return "redirect:/manage/event/" + savedEvent.getId()+ "/getting-stared";
+        return "redirect:/manage/event/" + savedEvent.getId()+ "/getting-started";
```

- Test chịu ảnh hưởng: mọi `redirectedUrl("/manage/event/{id}/getting-stared")` trong `EventControllerTest` cần đổi sang `getting-started`.
