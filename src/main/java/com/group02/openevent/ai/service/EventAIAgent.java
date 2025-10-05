package com.group02.openevent.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.ai.dto.Message;
import com.group02.openevent.ai.dto.TimeSlot;
import com.group02.openevent.ai.qdrant.model.ActionType;
import com.group02.openevent.ai.qdrant.model.PendingEvent;
import com.group02.openevent.ai.qdrant.model.TimeContext;
import com.group02.openevent.ai.qdrant.service.VectorIntentClassifier;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.PlaceService;
import com.group02.openevent.util.TimeSlotUnit;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EventAIAgent implements Serializable {
    
    private final LLM llm;
    private final List<Message> conversationHistory;
    private final Map<String, PendingEvent> pendingEvents = new HashMap<>();
    private final EmbeddingService embeddingService;
    private final PlaceService placeService;
    private final EventService eventService;
    private final AgentEventService agentEventService;
    private final VectorIntentClassifier classifier;
    private final WeatherService weatherService;

    public EventAIAgent(EmbeddingService embeddingService, 
                       PlaceService placeService,
                       EventService eventService,
                       AgentEventService agentEventService,
                       VectorIntentClassifier classifier,
                       WeatherService weatherService) {
        this.embeddingService = embeddingService;
        this.placeService = placeService;
        this.eventService = eventService;
        this.agentEventService = agentEventService;
        this.classifier = classifier;
        this.weatherService = weatherService;
        this.llm = new LLM();
        this.conversationHistory = new ArrayList<>();
        initializeSystemMessage();
    }

        private void initializeSystemMessage() {
            StringBuilder systemPrompt = new StringBuilder();

            systemPrompt.append("""
Bạn là một Trợ lý AI giúp người dùng quản lý sự kiện và luôn nhớ các ngày lễ và sự kiện quan trọng của Việt Nam.
Hãy hiểu ngôn ngữ tự nhiên, linh hoạt với các mô tả như "tối nay", "cuối tuần", v.v.

## MỤC TIÊU:
- Gợi ý, tạo, sửa, hoặc xoá sự kiện.
- Luôn phản hồi bằng văn bản tự nhiên (không hiện JSON).
- Nếu thiếu thông tin, hãy hỏi lại người dùng.
- Bạn hãy phản hồi dựa theo system message

## XỬ LÝ TẠO SỰ KIỆN:
1. Nếu người dùng yêu cầu tạo sự kiện:
- Khi người dùng yêu cầu tạo sự kiện (ví dụ: "Lên sự kiện", "Tạo sự kiện",...), bạn phải xác định và xuất hành động nội bộ là `ADD_EVENT`.
- Nếu thông tin sự kiện đầy đủ (tiêu đề, thời gian bắt đầu, thời gian kết thúc, địa điểm...), hãy xuất ra một JSON hành động nội bộ theo định dạng sau:            
   [
     {
       "toolName": "ADD_EVENT",
            "args": {
              "event_title": "...",
              "event_type": "...",
              "description": "...",
              "starts_at": "YYYY-MM-DDTHH:mm",
              "ends_at": "YYYY-MM-DDTHH:mm",
              "enroll_deadline": "YYYY-MM-DDTHH:mm",
              "public_date": "YYYY-MM-DDTHH:mm",
              "status": "...", 
              "image_url": "...",
              "benefits": "...",
              "learning_objects": "...",
              "points": ...,
              "competition_type": "...",
              "prize_pool": "...",
              "rules": "...",
              "culture": "...",
              "highlight": "...",
              "materials_link": "...",
              "topic": "...",
              "parent_event_id": null
       }
     }
   ]
- Không giải thích hay hiển thị nội dung JSON cho người dùng.
2. Nếu thời tiết có khả năng mưa (do hệ thống thời tiết trả về), hãy phản hồi như sau:
   - Ví dụ: "🌧 Thời tiết có thể có mưa vào thời gian này. Bạn có muốn tiếp tục tạo sự kiện ngoài trời này không?"
   - Nếu người dùng xác nhận "có", tiếp tục tạo sự kiện trước đó đang chờ (`PendingEvent`).
   - Nếu người dùng từ chối, không tạo sự kiện.

3. Nếu Địa điểm và thời gian yêu cầu trùng với sự kiện đã có:
   - **Không tự ý tạo sự kiện!**
   - Hỏi lại người dùng:  
     > "⏰ Thời gian bạn chọn đang bị trùng với một sự kiện khác. Bạn có muốn chọn thời gian hoặc địa điểm khác không?"

## KHI SỬA SỰ KIỆN:
- Khi người dùng nói các câu như:
    -"Thay đổi thời gian của sự kiện `workshop` lại"
    -"Sửa sự kiện `event_id` = 1"
    -"Update sự kiện"
    => Hiểu là người dùng muốn** Sửa sự kiện"
- Nếu có `event_id`, dùng nó.
- Nếu không, dùng `original_title` để tìm sự kiện cần sửa.
- Ví dụ:
[
  {
    "toolName": "UPDATE_EVENT",
    "args": {
      "event_id": 123,
      "original_title": "workshop cũ",
      "title": "workshop mới",
      "start_time": "YYYY-MM-DDTHH:mm",
      "description": "mô tả mới"
    }
  }
]
- Không giải thích hay hiển thị nội dung JSON cho người dùng.

## KHI XOÁ SỰ KIỆN:
- Khi người dùng nói các câu như:
    -"Xóa sự kiện `music` ngày 21 tháng 7"
    -"Xóa cuộc thi Hackathon"
    -"Xóa sự kiện `event_id` = 1"
    => Hiểu là người dùng muốn** Xóa sự kiện"
- Dùng `event_id` nếu có, hoặc `title` nếu không có ID.
- Ví dụ:
[
  { "toolName": "DELETE_EVENT", "args": { "event_id": 42 } }
]
hoặc
[
  { "toolName": "DELETE_EVENT", "args": { "title": "Tên sự kiện" } }
]
- Không giải thích hay hiển thị nội dung JSON cho người dùng.
                            

## NGUYÊN TẮC:
- Tránh dùng từ kỹ thuật với người dùng.
- Nếu phát hiện địa điểm và thời gian bị trùng với sự kiện khác, hãy hỏi lại người dùng một thời gian khác hoặc một địa điểm khác. Không tự ý thêm nếu bị trùng.
- Luôn diễn giải ý định rõ ràng, thân thiện.
- Ngày hiện tại là """ + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n");
// Không cần đoạn hiển thị mô tả lại nội dung cho người dùng nữa
            systemPrompt.append("- Nếu chưa rõ nội dung hoặc người dùng chưa xác nhận thời gian gợi ý, hãy hỏi lại người dùng trước khi trả về JSON.\n");

            systemPrompt.append("Các loại Event:\n");
            for (EventType type : EventType.values()) {
                systemPrompt.append("- ").append(type.name()).append("\n");
            }

            conversationHistory.add(new Message("system", systemPrompt.toString()));
        }


        /**
         * Process user input and generate AI response
         */
        @Transactional
        public String processUserInput(String userInput, int userId, HttpServletResponse response) throws Exception {
//        String intenttoolEvent = classifier.classiftoolEvent(userInput);
            boolean shouldReload = false;
            StringBuilder systemResult = new StringBuilder();
            if (pendingEvents.containsKey(String.valueOf(userId))) {
                String answer = userInput.trim().toLowerCase();

                if (answer.contains("có") || answer.contains("ok") || answer.contains("tiếp tục")) {
                    Event pending = pendingEvents.remove(String.valueOf(userId)).getEvent();
                    agentEventService.saveEvent(pending);
                    systemResult.append("📅 Đã tạo sự kiện: ").append(pending.getTitle());
                } else if (answer.contains("không")) {
                    pendingEvents.remove(String.valueOf(userId));
                    systemResult.append("❌ Đã hủy tạo sự kiện do bạn từ chối.");
                } else {
                    systemResult.append("❓Bạn có thể xác nhận lại: có/không?");
                }
                return systemResult.toString();
            }


            conversationHistory.add(new Message("user", userInput));
            String aiResponse = llm.generateResponse(conversationHistory);

            // After: String aiResponse = llm.generateResponse(conversationHistory);
            aiResponse = aiResponse
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "");

            // Extract JSON block (tool actions)
            Pattern jsonPattern = Pattern.compile("(\\[\\s*\\{[\\s\\S]*?\\}\\s*\\])");
            Matcher matcher = jsonPattern.matcher(aiResponse);
            String jsonPart = matcher.find() ? matcher.group() : null;

            // ✅ Text for user (LLM natural text without JSON)
            String userVisibleText = (jsonPart != null)
                    ? aiResponse.replace(jsonPart, "").trim()
                    : aiResponse.trim();

            // Parse actions after we have userVisibleText
            List<Action> actions = tryParseActions(jsonPart);

            if (actions != null && !actions.isEmpty()) {

                for (Action action : actions) {
                    String tool = action.getToolName();

                    try {
                        switch (tool) {
                            case "ADD_EVENT" -> {
                                if (!action.getArgs().containsKey("title")
                                        || !action.getArgs().containsKey("start_time")
                                        || !action.getArgs().containsKey("end_time")) {
                                    systemResult.append("📝 Thiếu thông tin sự kiện (tiêu đề hoặc thời gian).\n");
                                    continue;
                                }

                                String title = (String) action.getArgs().get("title");
                                String rawStart = (String) action.getArgs().get("start_time");
                                String rawEnd = (String) action.getArgs().get("end_time");

                                LocalDateTime start = tryParseDateTime(rawStart);
                                LocalDateTime end = tryParseDateTime(rawEnd);

                                // 1. Check trùng thời gian & địa điểm
                                String placeName = (String) action.getArgs().getOrDefault("place", "");
                                Optional<Place> placeOpt = placeService.findPlaceByName(placeName);

                                if (placeOpt.isPresent()) {
                                    List<Place> placeList = List.of(placeOpt.get()); // tạo list 1 phần tử
                                    List<Event> conflicted = eventService.isTimeConflict(start, end, placeList);

                                    if (!conflicted.isEmpty()) {
                                        systemResult.append("⚠️ Sự kiện bị trùng thời gian/địa điểm với:\n");
                                        for (Event conflict : conflicted) {
                                            systemResult.append(" - ").append(conflict.getTitle())
                                                    .append(" (").append(conflict.getStartsAt())
                                                    .append(" - ").append(conflict.getEndsAt()).append(")\n");
                                        }
                                        continue;
                                    }
                                }

                                Event event = new Event();

// Bắt buộc
                                event.setTitle(title);
                                event.setStartsAt(start);
                                event.setEndsAt(end);
                                event.setCreatedAt(LocalDateTime.now());
                                event.setEnrollDeadline(LocalDateTime.now().plusDays(1)); // mặc định hạn đăng ký +1 ngày
                                event.setStatus(EventStatus.DRAFT);   // mặc định khi tạo mới
                                event.setEventType(EventType.OTHERS); // mặc định nếu chưa phân loại

// Tùy chọn
                                if (action.getArgs().containsKey("description")) {
                                    event.setDescription((String) action.getArgs().get("description"));
                                }
                                if (action.getArgs().containsKey("image_url")) {
                                    event.setImageUrl((String) action.getArgs().get("image_url"));
                                }
                                if (action.getArgs().containsKey("benefits")) {
                                    event.setBenefits((String) action.getArgs().get("benefits"));
                                }


// Place (nhiều- nhiều)
                                if (action.getArgs().containsKey("place")) {
                                    String name = (String) action.getArgs().get("place");
                                    Place place = placeService.findPlaceByName(name)
                                            .orElse(null);
                                    if (place != null) {
                                        event.setPlaces(List.of(place));
                                    } else {
                                        systemResult.append("⚠️ Không tìm thấy địa điểm: ").append(placeName).append("\n");
                                        continue;
                                    }
                                }

// Parent Event
                                if (action.getArgs().containsKey("parent_event_id")) {
                                    Long parentId = (Long) action.getArgs().get("parent_event_id");
                                    Event parent = eventService.getEventByEventId(parentId).orElse(null);
                                    if (parent != null) {
                                        event.setParentEvent(parent);
                                    }
                                }

                                String intentWeather = classifier.classifyWeather(userInput);
                                if (intentWeather.equals("outdoor_activities")) {
                                    String forecastNote = weatherService.getForecastNote(start, "Da Nang");
                                    if (forecastNote != null && !forecastNote.isEmpty()) {
                                        pendingEvents.put("default", new PendingEvent(event));
                                        systemResult.append("🌦 ").append(forecastNote).append("\n").
                                                append("❓Bạn có muốn tiếp tục tạo sự kiện ngoài trời này không?").
                                                append("\n");
                                        continue;
                                    } else {

                                        System.out.println("⛅ Thời tiết tốt, tự động thêm sự kiện.");
                                    }

                                }
                                
                                try {
                                    agentEventService.saveEvent(event);
                                    systemResult.append("✅ Đã thêm sự kiện: ").append(title).append(" vào lịch trình.\n");
                                    shouldReload = true;
                                    System.out.println("✅ Event saved successfully: " + title + " with ID: " + event.getId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    systemResult.append("❌ Lỗi khi lưu sự kiện: ").append(e.getMessage()).append("\n");
                                    System.err.println("❌ Failed to save event: " + title + " - " + e.getMessage());
                                }

                            }

                            case "UPDATE_EVENT" -> {
                                Event existing = null;

                                // 1. Tìm sự kiện theo id hoặc title
                                if (action.getArgs().containsKey("event_id")) {
                                    long eventId = (long) action.getArgs().get("event_id");
                                    existing = eventService.getEventByEventId(eventId).orElse(null); // service trả về Optional<Event>
                                } else if (action.getArgs().containsKey("original_title")) {
                                    String oriTitle = (String) action.getArgs().get("original_title");
                                    existing = eventService.getFirstEventByTitle(oriTitle).orElse(null);
                                    System.out.println("🔎 Tìm theo title: " + oriTitle);
                                }

                                if (existing == null) {
                                    systemResult.append("❌ Không tìm thấy sự kiện để cập nhật.\n");
                                    continue;
                                }

                                // 2. Cập nhật các field cho event
                                if (action.getArgs().containsKey("title")) {
                                    existing.setTitle((String) action.getArgs().get("title"));
                                }
                                if (action.getArgs().containsKey("start_time")) {
                                    existing.setStartsAt(tryParseDateTime((String) action.getArgs().get("start_time")));
                                }
                                if (action.getArgs().containsKey("end_time")) {
                                    existing.setEndsAt(tryParseDateTime((String) action.getArgs().get("end_time")));
                                }
                                if (action.getArgs().containsKey("description")) {
                                    existing.setDescription((String) action.getArgs().get("description"));
                                }
                                if (action.getArgs().containsKey("image_url")) {
                                    existing.setImageUrl((String) action.getArgs().get("image_url"));
                                }
                                if (action.getArgs().containsKey("benefits")) {
                                    existing.setBenefits((String) action.getArgs().get("benefits"));
                                }

                                if (action.getArgs().containsKey("status")) {
                                    try {
                                        existing.setStatus(EventStatus.valueOf((String) action.getArgs().get("status")));
                                    } catch (IllegalArgumentException e) {
                                        systemResult.append("⚠️ Trạng thái không hợp lệ, giữ nguyên trạng thái cũ.\n");
                                    }
                                }

                                // 3. Cập nhật địa điểm (nếu có)
                                if (action.getArgs().containsKey("place")) {
                                    String placeName = (String) action.getArgs().get("place");
                                    Place place = placeService.findPlaceByName(placeName).orElse(null);
                                    if (place != null) {
                                        existing.setPlaces(List.of(place));
                                    } else {
                                        systemResult.append("⚠️ Không tìm thấy địa điểm: ").append(placeName).append("\n");
                                    }
                                }

                                // 4. Lưu lại sự kiện
                                eventService.saveEvent(existing);
                                systemResult.append("🔄 Đã cập nhật sự kiện: ").append(existing.getTitle()).append("\n");
                                shouldReload = true;
                            }

                            case "DELETE_EVENT" -> {
                                boolean deletedOne = false;
                                if (action.getArgs().containsKey("event_id")) {
                                    long id = (long) action.getArgs().get("event_id");
                                    deletedOne = eventService.removeEvent(id);
                                } else if (action.getArgs().containsKey("title")) {
                                    String title = (String) action.getArgs().get("title");
                                    deletedOne = eventService.deleteByTitle(title);
                                }

                                if (deletedOne) {
                                    shouldReload = true;
                                } else {
                                    systemResult.append("⚠️ Không tìm thấy sự kiện để xoá.\n");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        systemResult.append("❌ Lỗi khi xử lý hành động: ").append(tool).append("\n");
                    }
                }
            } else {
                // 2) No actions
                if (!userVisibleText.isBlank()) {
                    // LLM already gave a natural reply → return it, DO NOT run classifier
                    conversationHistory.add(new Message("assistant", userVisibleText));
                    return userVisibleText;
                }

                // 3) No natural text either → now run classifier & (maybe) fallback
                System.out.println("Không có hành động nào & không có câu trả lời tự nhiên. Đang phân loại ý định...");
                ActionType intent = classifier.classifyIntent(userInput);

                switch (intent) {
                    case PROMPT_FREE_TIME -> {
                        // 1. Xác định ngữ cảnh thời gian
                        TimeContext context = TimeSlotUnit.extractTimeContext(userInput);

                        // 2. Lấy Place từ userInput (nếu có)
                        String placeName = ""; // Place sẽ được extract từ userInput nếu cần
                        List<Event> busyEvents;
                        if (!placeName.isEmpty()) {
                            Place place = placeService.findPlaceByName(placeName)
                                    .orElse(null);
                            if (place != null) {
                                busyEvents = eventService.getEventsByPlace(place.getId());
                            } else {
                                systemResult.append("❌ Không tìm thấy địa điểm: ").append(placeName).append("\n");
                                break;
                            }
                        } else {
                            // Nếu không có địa điểm thì lấy tất cả
                            busyEvents = eventService.getAllEvents();
                        }

                        // 3. Lọc theo ngữ cảnh thời gian
                        List<Event> filteredEvents;
                        switch (context) {
                            case TODAY -> filteredEvents = TimeSlotUnit.filterEventsToday(busyEvents);
                            case TOMORROW -> filteredEvents = TimeSlotUnit.filterEventsTomorrow(busyEvents);
                            case THIS_WEEK -> filteredEvents = TimeSlotUnit.filterEventsThisWeek(busyEvents);
                            case NEXT_WEEK -> filteredEvents = TimeSlotUnit.filterEventsNextWeek(busyEvents);
                            default -> filteredEvents = busyEvents;
                        }

                        // 4. Tìm khoảng trống cho Place đó
                        List<TimeSlot> freeSlots = TimeSlotUnit.findFreeTime(filteredEvents);

                        // 5. Xuất kết quả
                        systemResult.append("📆 Các khoảng thời gian rảnh");
                        if (!placeName.isEmpty()) {
                            systemResult.append(" tại ").append(placeName);
                        }
                        systemResult.append(":\n");

                        if (freeSlots.isEmpty()) {
                            systemResult.append("❌ Không có khoảng thời gian rảnh trong ").append(context).append("\n");
                        } else {
                            for (TimeSlot slot : freeSlots) {
                                systemResult.append(" - ").append(slot.toString()).append("\n");
                            }
                        }
                    }

                    case PROMPT_SUMMARY_TIME -> {
                        try {
                            String summary = handleSummaryRequest(userInput, userId);
                            return (summary != null)
                                    ? summary
                                    : "📝 Mình không hiểu khoảng thời gian bạn muốn tổng hợp. Bạn có thể hỏi kiểu như: \"Lịch hôm nay\", \"Sự kiện tuần sau\"...";
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "⚠️ Đã xảy ra lỗi khi xử lý yêu cầu tổng hợp lịch.";
                        }
                    }
                    
                    case UNKNOWN, PROMPT_SEND_EMAIL, ERROR -> {
                        // Only add fallback when there is no useful text to show
                        systemResult.append("❓ Tôi không hiểu yêu cầu của bạn. Bạn có thể thử hỏi về việc tạo sự kiện, xem lịch, hoặc tìm thời gian rảnh.");
                    }
                }
            }
            // --- Combine & return as before (only when systemResult has content)
            if (!systemResult.isEmpty()) {
                String fullResponse = (userVisibleText + "\n\n" + systemResult.toString().trim()).trim();
                if (shouldReload) fullResponse += "\n__RELOAD__";
                conversationHistory.add(new Message("assistant", fullResponse));
                return fullResponse;
            }

            // No systemResult → return natural text (already stripped)
            conversationHistory.add(new Message("assistant", userVisibleText));
            return userVisibleText;
        }

        public static List<Action> tryParseActions(String jsonPart) {
            try {
                if (jsonPart == null || jsonPart.isEmpty()) {
                    return Collections.emptyList();
                }
                ObjectMapper objectMapper = new ObjectMapper();
                List<Action> list = Arrays.asList(objectMapper.readValue(jsonPart, Action[].class));
                System.out.println("✅ Parsed " + list.size() + " action(s).");
                return list;
            } catch (Exception e) {
                System.out.println("❌ Không thể parse Action(s): " + e.getMessage());
                System.out.println("📄 JSON:\n" + jsonPart);
                return Collections.emptyList();
            }
        }

        public String getGreeting() {
            List<Message> greetingMessages = new ArrayList<>();
            greetingMessages.add(conversationHistory.get(0));
            greetingMessages.add(new Message("user", "Xin chào, tôi cần tư vấn quản lý lịch trình"));

            try {
                return llm.generateResponse(greetingMessages);
            } catch (Exception e) {
                return "🤖 Xin chào! Tôi là AI Assistant quản lý lịch trình thông minh.\n"
                        + "Tôi có thể giúp bạn:\n"
                        + "✅ Tạo lịch học tập, công việc, sự kiện\n"
                        + "✅ Tối ưu hóa thời gian\n"
                        + "✅ Đưa ra lời khuyên quản lý thời gian\n\n"
                        + "Hãy chia sẻ kế hoạch của bạn để bắt đầu!";
            }
        }

        public boolean shouldEndConversation(String userInput) {
            String input = userInput.toLowerCase().trim();
            return input.equals("bye") || input.equals("tạm biệt")
                    || input.equals("kết thúc") || input.equals("quit")
                    || input.equals("exit") || input.equals("end");
        }

        public String getConversationSummary() {
            if (conversationHistory.size() <= 1) {
                return "Không có cuộc trò chuyện nào được ghi nhận.";
            }

            // Chỉ lấy các message từ user và assistant, bỏ qua system message
            List<Message> userMessages = conversationHistory.stream()
                    .filter(msg -> !msg.getRole().equals("system"))
                    .collect(java.util.stream.Collectors.toList());

            if (userMessages.size() <= 1) {
                return "📭 Chưa có cuộc trò chuyện thực sự nào được ghi nhận.";
            }

            StringBuilder summary = new StringBuilder("📌 TÓM TẮT CUỘC TRÒ CHUYỆN:\n");
            for (Message msg : userMessages) {
                summary.append(msg.getRole().equals("user") ? "🧑‍💻 Bạn: " : "🤖 AI: ")
                        .append(msg.getContent()).append("\n");
            }
            return summary.toString();
        }

//        public List<ScheduleItem> getCurrentSchedule(int userID) {
//
//            EventService eventService = new EventService();
//            List<UserEvents> userEvents = eventService.getAllEventsByUserId(userID);
//            List<ScheduleItem> schedules = new ArrayList<>();
//
//            for (UserEvents event : userEvents) {
//                String name = event.getName();
//                LocalDateTime start = event.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//                LocalDateTime end = event.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//
//                ScheduleItem item = new ScheduleItem(name, start, end, null);
//
//                // Ưu tiên hoặc màu có thể xác định priority
//                item.setPriority("Normal");
//
//                // Nếu bạn dùng Enum ScheduleType thì gán luôn:
//                item.setScheduleType(ScheduleItem.ScheduleType.EVENT);
//
//                schedules.add(item);
//            }
//
//            return schedules;
//        }

        private LocalDateTime tryParseDateTime(String input) {
            List<String> patterns = List.of(
                    "yyyy-MM-dd'T'HH:mm",
                    "yyyy-MM-dd HH:mm",
                    "dd/MM/yyyy HH:mm",
                    "dd-MM-yyyy HH:mm"
            );

            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return LocalDateTime.parse(input, formatter);
                } catch (Exception ignored) {
                }
            }

            throw new IllegalArgumentException("❌ Không thể parse ngày giờ: " + input);
        }

    /**
     * Kiểm tra thời gian rảnh cho người dùng
     */
    private String checkFreeTimes(String userInput, int userId) throws Exception {
        List<String> freeTimeIntents = Arrays.asList(
                "Gợi ý khung giờ học môn Toán",
                "Tôi muốn biết lúc nào rảnh để học",
                "Bạn có thể cho tôi biết thời gian trống để lên lịch?",
                "Tìm khoảng thời gian rảnh trong tuần",
                "Lên lịch học phù hợp giúp tôi",
                "Hãy đề xuất giờ học hợp lý"
        );

        // 1. Vector embedding của input
        float[] inputVec = embeddingService.getEmbedding(userInput);

        // 2. Kiểm tra intent có phải là "tìm free time"
        boolean isGetFreeTimeIntent = false;
        for (String example : freeTimeIntents) {
            float[] refVec = embeddingService.getEmbedding(example);
            if (embeddingService.cosineSimilarity(inputVec, refVec) > 0.82f) {
                isGetFreeTimeIntent = true;
                break;
            }
        }

        // 3. Nếu đúng intent → xử lý
        if (isGetFreeTimeIntent) {
            // 📌 Lấy tất cả sự kiện của user
            List<Event> events = eventService.getAllEvents(); // TODO: Implement getEventByUserId method

            // 📌 Tìm khoảng trống
            List<TimeSlot> freeSlots = TimeSlotUnit.findFreeTime(events);

            if (freeSlots.isEmpty()) {
                return "⛔ Hiện bạn không có khoảng thời gian trống nào trong tuần.";
            }

            StringBuilder response = new StringBuilder("📅 Các khoảng thời gian trống gợi ý:\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE - dd/MM/yyyy HH:mm");

            for (TimeSlot slot : freeSlots) {
                response.append("• ")
                        .append(slot.getStart().format(formatter))
                        .append(" → ")
                        .append(slot.getEnd().format(formatter))
                        .append("\n");
            }

            return response.toString();
        }

        return null;
    }

    public String handleSummaryRequest(String userInputs, int userId) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        String range = null;

        if (userInputs.contains("hôm nay")) {
            start = now.toLocalDate().atStartOfDay();
            range = "hôm nay";
        } else if (userInputs.contains("ngày mai")) {
            start = now.plusDays(1).toLocalDate().atStartOfDay();
            range = "ngày mai";
        } else if (userInputs.contains("tuần này")) {
            DayOfWeek dow = now.getDayOfWeek();
            start = now.minusDays(dow.getValue() - 1).toLocalDate().atStartOfDay(); // Monday
            range = "tuần này";
        } else if (userInputs.contains("tuần sau")) {
            DayOfWeek dow = now.getDayOfWeek();
            start = now.minusDays(dow.getValue() - 1).toLocalDate().atStartOfDay().plusWeeks(1);
            range = "tuần sau";
        } else {
            return null;
        }

        // 📌 Lấy sự kiện theo khoảng thời gian và user
        List<Event> allEvents = eventService.getAllEvents();
        // TODO: Implement proper filtering by date range and user
        List<Event> events = allEvents.stream()
                .filter(event -> event.getStartsAt().isAfter(start) || event.getStartsAt().isEqual(start))
                .collect(java.util.stream.Collectors.toList());

        if (events.isEmpty()) {
            return "📭 Không có sự kiện nào trong " + range + ".";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append("📆 Các sự kiện ").append(range).append(":\n");
        for (Event e : events) {
            sb.append("• ").append(e.getTitle())   // ⚡ dùng đúng field trong entity
                    .append(" 🕒 ")
                    .append(e.getStartsAt().format(formatter))
                    .append(" - ")
                    .append(e.getEndsAt().format(formatter));

            if (e.getPlaces() != null && !e.getPlaces().isEmpty()) {
                sb.append(" 📍 ").append(e.getPlaces().get(0).getPlaceName());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
