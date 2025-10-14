package com.group02.openevent.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.ai.dto.Message;
import com.group02.openevent.ai.dto.TimeSlot;
import com.group02.openevent.ai.qdrant.model.ActionType;
import com.group02.openevent.ai.qdrant.model.PendingEvent;
import com.group02.openevent.ai.qdrant.model.TimeContext;
import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.ai.qdrant.service.VectorIntentClassifier;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.PlaceService;
import com.group02.openevent.service.TicketTypeService;
import com.group02.openevent.util.TimeSlotUnit;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class EventAIAgent implements Serializable {

    private final LLM llm;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final PlaceService placeService;
    private final EventService eventService;
    private final AgentEventService agentEventService;
    private final VectorIntentClassifier classifier;
    private final WeatherService weatherService;
    private final List<Message> conversationHistory;
    private final EventVectorSearchService eventVectorSearchService;
    private final OrderAIService orderAIService;
    private final IUserRepo userRepo;
    private final TicketTypeService ticketTypeService;
    private final Map<String, PendingEvent> pendingEvents = new HashMap<>();

    public EventAIAgent(EmbeddingService embeddingService,
                        PlaceService placeService,
                        EventService eventService,
                        AgentEventService agentEventService,
                        VectorIntentClassifier classifier,
                        WeatherService weatherService,
                        LLM llm,QdrantService qdrantService,
                        EventVectorSearchService eventVectorSearchService,
                        OrderAIService orderAIService,
                        IUserRepo userRepo,
                        TicketTypeService ticketTypeService) {

        this.embeddingService = embeddingService;
        this.placeService = placeService;
        this.eventService = eventService;
        this.agentEventService = agentEventService;
        this.classifier = classifier;
        this.weatherService = weatherService;
        this.eventVectorSearchService = eventVectorSearchService;
        this.llm = llm;
        this.qdrantService = qdrantService;
        this.orderAIService = orderAIService;
        this.userRepo = userRepo;
        this.ticketTypeService = ticketTypeService;
        // Khởi tạo các trường non-final
        this.conversationHistory = new ArrayList<>();
        // this.pendingEvents đã được khởi tạo ở trên
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
             "starts_at": "YYYY-MM-DDTHH:mm", 
             "ends_at": "YYYY-MM-DDTHH:mm",    
             "place": "...",                 
             "description": "...",
             "event_type": "...",       
             "capacity": 100,
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
- Luôn đảm bảo rằng các trường TÊN SỰ KIỆN, THỜI GIAN BẮT ĐẦU/KẾT THÚC, và ĐỊA ĐIỂM đều được xác định. Nếu bất kỳ trường nào bị thiếu, hãy hỏi lại người dùng.
- Nếu phát hiện địa điểm và thời gian bị trùng với sự kiện khác, hãy hỏi lại người dùng một thời gian khác hoặc một địa điểm khác. Không tự ý thêm nếu bị trùng.
- Luôn diễn giải ý định rõ ràng, thân thiện.
- Ngày hiện tại là """ + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n");
// Không cần đoạn hiển thị mô tả lại nội dung cho người dùng nữa
            systemPrompt.append("- Nếu chưa rõ nội dung hoặc người dùng chưa xác nhận thời gian gợi ý, hãy hỏi lại người dùng trước khi trả về JSON.\n");

            systemPrompt.append("Các loại Event:\n");
            for (EventType type : EventType.values()) {
                systemPrompt.append("- ").append(type.name()).append("\n");
            }

            systemPrompt.append("""

## XỬ LÝ MUA VÉ SỰ KIỆN:
1. Khi người dùng muốn mua vé (ví dụ: "Mua vé sự kiện X", "Đăng ký tham gia Y", "Đặt vé Z"):
   - Hệ thống sẽ tự động tìm sự kiện theo tên
   - Hiển thị danh sách loại vé có sẵn với giá và số lượng còn lại
   - Hướng dẫn người dùng chọn loại vé phù hợp

2. Khi người dùng chọn loại vé:
   - Xác nhận loại vé đã chọn và giá
   - Yêu cầu thông tin người tham gia: tên, email, số điện thoại

3. Khi người dùng cung cấp thông tin:
   - Trích xuất các thông tin: tên, email, SĐT
   - Hiển thị tóm tắt đơn hàng đầy đủ
   - Yêu cầu xác nhận cuối cùng (Có/Không)

4. Khi người dùng xác nhận:
   - Hệ thống tự động tạo đơn hàng
   - Tạo payment link qua PayOS
   - Trả về link thanh toán cho người dùng

**LƯU Ý QUAN TRỌNG VỀ MUA VÉ:**
- Quy trình mua vé được xử lý TỰ ĐỘNG bởi hệ thống
- KHÔNG cần xuất JSON cho chức năng mua vé
- Chỉ trả lời tự nhiên và hướng dẫn người dùng
- Nếu thiếu thông tin, hỏi lại người dùng một cách thân thiện
- Nếu vé đã hết, đề xuất các loại vé khác còn sẵn
- Luôn xác nhận lại trước khi tạo đơn hàng

## XỬ LÝ GỬI EMAIL NHẮC NHỞ:
1. Khi người dùng yêu cầu gửi email nhắc nhở (ví dụ: "Nhắc tôi về sự kiện X trước 30 phút", "Gửi email trước 1 giờ"):
   - Hệ thống sẽ tự động tìm sự kiện theo tên hoặc sử dụng sự kiện sắp tới
   - Trích xuất thời gian nhắc nhở (phút/giờ)
   - Lưu yêu cầu nhắc nhở vào hệ thống
   - Xác nhận với người dùng về việc sẽ gửi email

2. Hệ thống sẽ tự động gửi email nhắc nhở trước thời gian sự kiện bắt đầu
   - Email sẽ được gửi đến địa chỉ email của người dùng
   - Nội dung email bao gồm thông tin sự kiện và thời gian

**LƯU Ý QUAN TRỌNG VỀ EMAIL:**
- Chức năng gửi email nhắc nhở có sẵn và hoạt động
- KHÔNG cần xuất JSON cho chức năng email
- Chỉ trả lời tự nhiên và xác nhận với người dùng
- Hệ thống sẽ tự động xử lý việc gửi email đúng thời gian
""");

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
            
            // Check pending event confirmation first
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

            float[] userVector = embeddingService.getEmbedding(userInput);

            // ==================== ORDER FLOW HANDLING ====================
            
            // Check if user wants to buy tickets using VectorIntentClassifier
            ActionType intent = classifier.classifyIntent(userInput, userVector);
            if (intent == ActionType.BUY_TICKET) {
                // ✅ SỬA LẠI ĐỂ GỌI SERVICE CHUYÊN DỤNG
                List<Event> foundEvents = eventVectorSearchService.searchEvents(userInput, userId, 1);

                if (foundEvents.isEmpty()) {
                    return "Tôi hiểu bạn muốn mua vé, nhưng tôi chưa nhận ra tên sự kiện. Bạn có thể nói rõ hơn được không, ví dụ: 'Mua vé sự kiện Music Night'";
                }

                String eventName = foundEvents.get(0).getTitle();

                // Phần còn lại giữ nguyên
                Optional<Event> eventOpt = eventService.getFirstPublicEventByTitle(eventName.trim());
                if (eventOpt.isEmpty()) {
                    return "❌ Không tìm thấy sự kiện \"" + eventName.trim() + "\" đang mở bán vé. Vui lòng kiểm tra lại tên sự kiện.";
                }

                return orderAIService.startOrderCreation((long) userId, eventName.trim());
            }

            // Check if user is in pending order flow
            if (orderAIService.hasPendingOrder((long) userId)) {
                com.group02.openevent.ai.dto.PendingOrder pendingOrder = orderAIService.getPendingOrder((long) userId);
                
                // Handle based on current step
                switch (pendingOrder.getCurrentStep()) {
                    case SELECT_EVENT -> {
                        // Should not happen, but handle gracefully
                        return "ℹ️ Vui lòng cho biết tên sự kiện bạn muốn mua vé.";
                    }
                    case SELECT_TICKET_TYPE -> {
                        // User is selecting ticket type
                        return orderAIService.selectTicketType((long) userId, userInput);
                    }
                    case PROVIDE_INFO -> {
                        // User is providing info
                        Map<String, String> info = extractParticipantInfo(userInput);
                        return orderAIService.provideInfo((long) userId, info);
                    }
                    case CONFIRM_ORDER -> {
                        // Use VectorIntentClassifier to understand confirm/cancel intent
                        ActionType confirmIntent = classifier.classifyConfirmIntent(userInput, userVector);
                        
                        switch (confirmIntent) {
                            case CONFIRM_ORDER -> {
                                Map<String, Object> result = orderAIService.confirmOrder((long) userId);
                                return (String) result.get("message");
                            }
                            case CANCEL_ORDER -> {
                                return orderAIService.cancelOrder((long) userId);
                            }
                            case UNKNOWN -> {
                                return "❓ Tôi không hiểu rõ ý của bạn. Vui lòng trả lời rõ ràng:\n" +
                                       "• 'Có' hoặc 'Đồng ý' để xác nhận đơn hàng\n" +
                                       "• 'Không' hoặc 'Hủy' để hủy đơn hàng";
                            }
                            default -> {
                                return "❓ Tôi không hiểu rõ ý của bạn. Vui lòng trả lời rõ ràng:\n" +
                                       "• 'Có' hoặc 'Đồng ý' để xác nhận đơn hàng\n" +
                                       "• 'Không' hoặc 'Hủy' để hủy đơn hàng";
                            }
                        }
                    }
                }
            }
            
            // ==================== END ORDER FLOW ====================

            conversationHistory.add(new Message("user", userInput));
            
            // Check for ticket info query BEFORE calling LLM
            if (classifier.isTicketInfoQuery(userInput, userVector)) {
                System.out.println("🎯 DEBUG: Detected ticket info query, bypassing LLM and using database logic");
                return handleTicketInfoQuery(userInput, userVector);
            }
            
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
                                Map<String, Object> args = action.getArgs();
//                                System.out.println("Action keys: " + args.keySet()+"Raw args: " + args);
//                                System.out.println("📝 Thiếu thông tin sự kiện (tiêu đề hoặc thời gian).\n");
//
//
//                                if (!action.getArgs().containsKey("title")
//                                        || !action.getArgs().containsKey("start_time")
//                                        || !action.getArgs().containsKey("end_time")) {
//                                    systemResult.append("Action keys: " + args.keySet()+"Raw args: " + args);
//                                    continue;
//                                }

                                String title = getStr(args, "title", "event_title", "name");
                                LocalDateTime start = getTime(args, "start_time", "starts_at", "start", "from", "begin");
                                LocalDateTime end   = getTime(args, "end_time", "ends_at", "end", "to", "finish");

                                if (title == null || start == null || end == null) {
                                    systemResult.append("📝 Thiếu thông tin sự kiện (tiêu đề hoặc thời gian).\n");
                                    continue;
                                }

// Validate thời gian
                                if (!start.isBefore(end)) {
                                    systemResult.append("⛔ Thời gian không hợp lệ: bắt đầu phải trước kết thúc.\n");
                                    continue;
                                }

                                // 1. Check trùng thời gian & địa điểm
                                String placeName = getStr(args, "place", "location"); // Lấy tên địa điểm thô từ LLM
                                Optional<Place> placeOpt = Optional.empty(); // Khởi tạo Optional rỗng

                                if (placeName != null) {
                                    // 📌 BỔ SUNG: Dùng Vector Search (Qdrant) để tìm địa điểm chuẩn hóa nhất (giải quyết nhầm lẫn)
                                    try {
                                        float[] placeVec = embeddingService.getEmbedding(placeName);
                                        // Đây là phương thức giả định. Bạn cần code nó trong QdrantService
                                        List<Map<String, Object>> searchResults = qdrantService.searchPlacesByVector(placeVec, 1);

                                        if (!searchResults.isEmpty()) {
                                            Map<String, Object> result = searchResults.get(0);
                                            Object placeIdObj = result.get("place_id"); // Lấy ra dưới dạng Object trước

                                            if (placeIdObj instanceof Number) { // Kiểm tra xem có phải là số không
                                                Long placeId = ((Number) placeIdObj).longValue();
                                                placeOpt = placeService.findPlaceById(placeId);
                                            } else {
                                                // Nếu place_id là null hoặc không phải là số, ghi log và fallback
                                                log.warn("Qdrant result for '{}' is missing or has invalid place_id. Falling back to DB flexible search.", placeName);
                                                placeOpt = placeService.findPlaceByNameFlexible(placeName);
                                            }
                                        } else {
                                            // Không có kết quả từ Qdrant, fallback sang tìm kiếm DB linh hoạt
                                            log.debug("No Qdrant results for '{}', falling back to DB flexible search.", placeName);
                                            placeOpt = placeService.findPlaceByNameFlexible(placeName);
                                        }
                                    } catch (Exception e) {
                                        log.error("Qdrant Place search failed (Falling back to DB flexible search): {}", e.getMessage());

                                        // Chuyển sang tìm kiếm DB linh hoạt
                                        placeOpt = placeService.findPlaceByNameFlexible(placeName);
                                    }
                                }

                                if (placeOpt.isPresent()) {
                                    // 1. Nếu tìm thấy địa điểm, tiến hành kiểm tra xung đột
                                    List<Place> placeList = List.of(placeOpt.get()); // tạo list 1 phần tử
                                    List<Event> conflicted = eventService.isTimeConflict(start, end, placeList);

                                    if (!conflicted.isEmpty()) {
                                        // 1a. Xung đột -> Báo lỗi và NGẮT
                                        systemResult.append("⚠️ Sự kiện bị trùng thời gian/địa điểm với:\n");
                                        for (Event conflict : conflicted) {
                                            systemResult.append(" - ").append(conflict.getTitle())
                                                    .append(" (").append(conflict.getStartsAt())
                                                    .append(" - ").append(conflict.getEndsAt()).append(")\n");
                                        }
                                        continue; // Ngắt luồng ADD_EVENT hiện tại
                                    }
                                    // 1b. Không xung đột -> Tiếp tục xuống khối tạo Event

                                } else {
                                    // 2. KHÔNG tìm thấy địa điểm -> Báo lỗi và NGẮT (Vì địa điểm là BẮT BUỘC)

                                    // TẠO THÔNG BÁO LỖI BẮT BUỘC:
                                    systemResult.append("⛔ Để tạo sự kiện, bạn cần cung cấp địa điểm hợp lệ.");

                                    // Lấy tên địa điểm thô từ args để thông báo cụ thể hơn
                                    String placeNameRaw = getStr(action.getArgs(), "place", "location");
                                    if (placeNameRaw != null && !placeNameRaw.isBlank()) {
                                        systemResult.append(" Không tìm thấy địa điểm \"").append(placeNameRaw).append("\".\n");
                                    } else {
                                        systemResult.append(" Vui lòng cung cấp tên địa điểm.\n");
                                    }

                                    continue; // Ngắt luồng ADD_EVENT hiện tại
                                }

                                Event event = new Event();

// Bắt buộc
                                event.setTitle(title);
                                event.setStartsAt(start);
                                event.setEndsAt(end);
                                event.setCreatedAt(LocalDateTime.now());
                                LocalDateTime defaultDeadline = start.minusHours(1); // Mặc định deadline 1 giờ trước khi bắt đầu
                                if (action.getArgs().containsKey("enroll_deadline")) {
                                    // Lấy thời gian deadline từ args nếu có
                                    LocalDateTime deadline = getTime(args, "enroll_deadline", "deadline"); // Cần hàm getTime
                                    if (deadline != null && deadline.isBefore(start)) {
                                        event.setEnrollDeadline(deadline);
                                    } else {
                                        event.setEnrollDeadline(defaultDeadline);
                                    }
                                } else {
                                    event.setEnrollDeadline(defaultDeadline);
                                }
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
                                if (placeOpt.isPresent()) {
                                    event.setPlaces(List.of(placeOpt.get()));
                                }

// Parent Event
                                if (action.getArgs().containsKey("parent_event_id")) {
                                    Long parentId = (Long) action.getArgs().get("parent_event_id");
                                    Event parent = eventService.getEventByEventId(parentId).orElse(null);
                                    if (parent != null) {
                                        event.setParentEvent(parent);
                                    }
                                }

                                String intentWeather = classifier.classifyWeather(userInput, userVector);
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
                                
                                // Get organization_id if provided
                                Long orgId = null;
                                if (action.getArgs().containsKey("organization_id") && action.getArgs().get("organization_id") != null) {
                                    orgId = Long.valueOf(action.getArgs().get("organization_id").toString());
                                }

                                try {
                                    log.info("🔍 Creating event with title: {}, userId: {}, orgId: {}", 
                                            event.getTitle(), userId, orgId);
                                    
                                    // Use createEventByCustomer to handle host creation and organization assignment
                                    Event saved = agentEventService.createEventByCustomer((long) userId, event, orgId);
                                    systemResult.append("✅ Đã thêm sự kiện: ").append(saved.getTitle())
                                            .append(saved.getOrganization() != null
                                                    ? " (Org: " + saved.getOrganization().getOrgName() + ")"
                                                    : " (không gắn Organization)")
                                            .append("\n");
                                    shouldReload = true;
                                    System.out.println("✅ Event saved successfully: " + saved.getTitle() + " with ID: " + saved.getId());
                                    try {
                                        log.info("Upserting event vector to Qdrant for event ID: {}", saved.getId());

                                        // 1. Tạo vector từ tiêu đề sự kiện
                                        float[] eventVector = embeddingService.getEmbedding(saved.getTitle());

                                        // 2. Chuẩn bị payload cho Qdrant
                                        Map<String, Object> payload = new HashMap<>();
                                        payload.put("event_id", saved.getId());
                                        payload.put("title", saved.getTitle());
                                        payload.put("kind", "event"); // Rất quan trọng cho việc lọc sau này
                                        payload.put("startsAt", saved.getStartsAt().toEpochSecond(java.time.ZoneOffset.UTC)); // Chuyển thành Unix timestamp

                                        // 3. Gọi service để upsert
                                        qdrantService.upsertEmbedding(String.valueOf(saved.getId()), eventVector, payload);

                                        log.info("✅ Successfully upserted event vector for '{}'", saved.getTitle());

                                    } catch (Exception qdrantEx) {
                                        log.error("❌ Failed to upsert event vector to Qdrant for event ID {}: {}", saved.getId(), qdrantEx.getMessage());
                                        // Không cần ném lỗi ra ngoài, chỉ cần ghi log
                                        // Việc không đồng bộ được vector không nên làm hỏng luồng tạo sự kiện chính
                                    }
                                } catch (Exception e) {
                                    log.error("❌ Error creating event: {}", e.getMessage(), e);
                                    e.printStackTrace();
                                    systemResult.append("❌ Lỗi khi lưu sự kiện: ").append(e.getMessage()).append("\n");
                                    System.err.println("❌ Failed to save event: " + title + " - " + e.getMessage());
                                }

                            }

                            case "UPDATE_EVENT" -> {
                                Event existing = null;
                                Map<String,Object> args = action.getArgs();

                                if (!args.containsKey("event_id") && !args.containsKey("original_title")) {
                                    systemResult.append("❌ Thiếu định danh sự kiện. Hãy cung cấp `event_id` hoặc `original_title`.\n");
                                    break;
                                }

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
                                if (args.containsKey("start_time") || args.containsKey("starts_at")) {
                                    existing.setStartsAt(getTime(args,"start_time","starts_at","start","from","begin"));
                                }
                                if (args.containsKey("end_time") || args.containsKey("ends_at")) {
                                    existing.setEndsAt(getTime(args,"end_time","ends_at","end","to","finish"));
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
                    System.out.println("🤖 DEBUG: LLM returned natural text, bypassing database logic: '" + userVisibleText.substring(0, Math.min(100, userVisibleText.length())) + "...'");
                    conversationHistory.add(new Message("assistant", userVisibleText));
                    return userVisibleText;
                }

                // 3) No natural text either → now run classifier & (maybe) fallback
                System.out.println("Không có hành động nào & không có câu trả lời tự nhiên. Đang phân loại ý định...");
                ActionType fallbackIntent = classifier.classifyIntent(userInput,userVector);

                switch (fallbackIntent) {
                    case BUY_TICKET -> {
                        // This should already be handled above, but just in case
                        return "❌ Vui lòng bắt đầu lại quy trình mua vé bằng cách nói 'Mua vé [tên sự kiện]'";
                    }
                    case CONFIRM_ORDER, CANCEL_ORDER -> {
                        return "❌ Không có đơn hàng nào đang chờ xác nhận. Vui lòng bắt đầu quy trình mua vé trước.";
                    }
                    case QUERY_TICKET_INFO -> {
                        // Xử lý câu hỏi về thông tin vé
                        return handleTicketInfoQuery(userInput, userVector);
                    }
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
                    case PROMPT_SEND_EMAIL -> {
                        // 1. TRÍCH XUẤT THỜI GIAN NHẮC NHỞ
                        Pattern patternTime = Pattern.compile("trước (\\d{1,3}) ?(phút|giờ)");
                        Matcher matcherTime = patternTime.matcher(userInput.toLowerCase());

                        if (!matcherTime.find()) {
                            systemResult.append("❓ Bạn muốn tôi nhắc trước bao nhiêu phút hoặc giờ? (Ví dụ: 'trước 45 phút').");
                            break; // Ngắt nếu không có thời gian
                        }

                        int value = Integer.parseInt(matcherTime.group(1));
                        String unit = matcherTime.group(2);
                        int remindMinutes = unit.equals("giờ") ? value * 60 : value;

                        // ***************************************************************
                        // 2. TÌM KIẾM SỰ KIỆN CỤ THỂ BẰNG VECTOR SEARCH
                        // ***************************************************************

                        // Giả định: IntentClassifier cũng có phương thức classifyEventTitle(userInput)
                        // để trích xuất tên sự kiện (dùng LLM hoặc Regex phức tạp hơn)
                        String eventTitle = classifier.classifyEventTitle(userInput, userVector);
                        Optional<Event> targetEventOpt = Optional.empty();

                        if (!eventTitle.isBlank()) {
                            // TÌM KIẾM THEO TÊN NẾU CÓ: Sử dụng Vector Search để tìm sự kiện chính xác nhất
                            try {
                                List<Event> results = eventVectorSearchService.searchEvents(eventTitle, userId, 1);
                                if (!results.isEmpty()) {
                                    targetEventOpt = Optional.of(results.get(0));
                                }
                            } catch (Exception e) {
                                log.error("Vector search failed for event title: {}", e.getMessage());
                            }
                        }

                        // ***************************************************************
                        // 3. FALLBACK VÀ LƯU NHẮC NHỞ
                        // ***************************************************************

                        // Nếu không tìm thấy sự kiện cụ thể, FALLBACK về sự kiện sắp nhất
                        if (targetEventOpt.isEmpty()) {
                            targetEventOpt = eventService.getNextUpcomingEventByUserId((long) userId);

                            if (targetEventOpt.isEmpty()) {
                                systemResult.append("❓ Mình không tìm thấy sự kiện cụ thể nào trong yêu cầu hoặc sự kiện sắp tới nào trong lịch của bạn.");
                                break;
                            }
                        }

                        // 4. KIỂM TRA EMAIL CỦA USER TRƯỚC KHI LƯU NHẮC NHỞ
                        Event finalEvent = targetEventOpt.get();
                        System.out.println("🔍 DEBUG: Found event: " + finalEvent.getTitle() + " (ID: " + finalEvent.getId() + ")");

                        try {
                            // Lấy thông tin customer từ userId để kiểm tra email
                            System.out.println("🔍 DEBUG: Looking for user with account ID: " + userId);
                            Optional<Customer> customerOpt = userRepo.findByAccount_AccountId((long) userId);
                            
                            if (customerOpt.isEmpty() || customerOpt.get().getAccount() == null) {
                                System.out.println("❌ DEBUG: User not found or account is null");
                                systemResult.append("❌ Không tìm thấy thông tin tài khoản của bạn. Vui lòng đăng nhập lại.");
                                break;
                            }
                            
                            String userEmail = customerOpt.get().getAccount().getEmail();
                            System.out.println("🔍 DEBUG: User email: " + userEmail);
                            
                            if (userEmail == null || userEmail.trim().isEmpty()) {
                                System.out.println("❌ DEBUG: User email is null or empty");
                                systemResult.append("❌ Tài khoản của bạn chưa có địa chỉ email. Vui lòng cập nhật email trong thông tin cá nhân.");
                                break;
                            }

                            // Lưu nhắc nhở email vào DB
                            System.out.println("🔍 DEBUG: Saving reminder for event ID: " + finalEvent.getId() + ", user ID: " + userId + ", minutes: " + remindMinutes);
                            agentEventService.saveEmailReminder(finalEvent.getId(), remindMinutes, (long) userId);
                            System.out.println("✅ DEBUG: Reminder saved successfully!");

                            return "✅ Tôi sẽ gửi email nhắc bạn về sự kiện \"" + finalEvent.getTitle() +
                                    "\" trước " + remindMinutes + " phút khi sự kiện bắt đầu.\n" +
                                    "📧 Email sẽ được gửi đến: " + userEmail;

                        } catch (Exception e) {
                            System.out.println("❌ DEBUG: Exception when saving reminder: " + e.getMessage());
                            e.printStackTrace();
                            systemResult.append("❌ Lỗi khi lưu nhắc nhở email: ").append(e.getMessage()).append("\n");
                        }
                    }
                    
                    case UNKNOWN, ERROR -> {
                        // Only add fallback when there is no useful text to show
                        systemResult.append("❓ Tôi không hiểu yêu cầu của bạn. Bạn có thể thử hỏi về việc tạo sự kiện, xem sự kiện, hoặc nhắc bạn bằng gmail.");
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

        public List<Event> getCurrentEvent(Long userId) {
            // Lấy tất cả event của user (bạn đã có EventService.getEventByUserId)
            List<Event> events = eventService.getEventByUserId(userId);
            LocalDate today = LocalDate.now();

            // Giữ các event mà hôm nay nằm trong khoảng start..end
            return events.stream()
                    .filter(e -> {
                        LocalDate start = e.getStartsAt().toLocalDate();
                        LocalDate end   = e.getEndsAt().toLocalDate();
                        return !start.isAfter(today) && !end.isBefore(today);
                    })
                    .sorted(Comparator.comparing(Event::getStartsAt))
                    .toList();
        }

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
//    private String checkFreeTimes(String userInput, int userId) throws Exception {
//        List<String> freeTimeIntents = Arrays.asList(
//                "Gợi ý khung giờ học môn Toán",
//                "Tôi muốn biết lúc nào rảnh để học",
//                "Bạn có thể cho tôi biết thời gian trống để lên lịch?",
//                "Tìm khoảng thời gian rảnh trong tuần",
//                "Lên lịch học phù hợp giúp tôi",
//                "Hãy đề xuất giờ học hợp lý"
//        );
//
//        // 1. Vector embedding của input
//        float[] inputVec = embeddingService.getEmbedding(userInput);
//
//        // 2. Kiểm tra intent có phải là "tìm free time"
//        boolean isGetFreeTimeIntent = false;
//        for (String example : freeTimeIntents) {
//            float[] refVec = embeddingService.getEmbedding(example);
//            if (embeddingService.cosineSimilarity(inputVec, refVec) > 0.82f) {
//                isGetFreeTimeIntent = true;
//                break;
//            }
//        }
//
//        // 3. Nếu đúng intent → xử lý
//        if (isGetFreeTimeIntent) {
//            // 📌 Lấy tất cả sự kiện của user
//            List<Event> events = eventService.getAllEvents(); // TODO: Implement getEventByUserId method
//
//            // 📌 Tìm khoảng trống
//            List<TimeSlot> freeSlots = TimeSlotUnit.findFreeTime(events);
//
//            if (freeSlots.isEmpty()) {
//                return "⛔ Hiện bạn không có khoảng thời gian trống nào trong tuần.";
//            }
//
//            StringBuilder response = new StringBuilder("📅 Các khoảng thời gian trống gợi ý:\n");
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE - dd/MM/yyyy HH:mm");
//
//            for (TimeSlot slot : freeSlots) {
//                response.append("• ")
//                        .append(slot.getStart().format(formatter))
//                        .append(" → ")
//                        .append(slot.getEnd().format(formatter))
//                        .append("\n");
//            }
//
//            return response.toString();
//        }
//
//        return null;
//    }

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
    
    /**
     * Extract event name from user input for order creation
     */
    private String extractEventName(String userInput) {
        // Remove common buy ticket keywords
        String cleaned = userInput
            .replaceAll("(?i)(mua vé|mua ve|đăng ký|đăng ky|tham gia|đặt vé|dat ve|book vé|order vé|sự kiện|su kien|event)", "")
            .trim();
        return cleaned;
    }
    
    /**
     * Extract participant information from user input for order
     */
    private Map<String, String> extractParticipantInfo(String userInput) {
        Map<String, String> info = new HashMap<>();
        
        try {
            // Extract name
            Pattern namePattern = Pattern.compile("(?:tên|ten|họ tên|ho ten|name)\\s*:?\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
            Matcher nameMatcher = namePattern.matcher(userInput);
            if (nameMatcher.find()) {
                info.put("name", nameMatcher.group(1).trim());
            }
            
            // Extract email
            Pattern emailPattern = Pattern.compile("(?:email|mail|e-mail)\\s*:?\\s*([^,\\s]+@[^,\\s]+)", Pattern.CASE_INSENSITIVE);
            Matcher emailMatcher = emailPattern.matcher(userInput);
            if (emailMatcher.find()) {
                info.put("email", emailMatcher.group(1).trim());
            }
            
            // Extract phone
            Pattern phonePattern = Pattern.compile("(?:sđt|sdt|phone|số điện thoại|so dien thoai|điện thoại|dien thoai)\\s*:?\\s*([0-9]{9,11})", Pattern.CASE_INSENSITIVE);
            Matcher phoneMatcher = phonePattern.matcher(userInput);
            if (phoneMatcher.find()) {
                info.put("phone", phoneMatcher.group(1).trim());
            }
            
            // Extract organization (optional)
            Pattern orgPattern = Pattern.compile("(?:tổ chức|to chuc|organization|công ty|cong ty|trường|truong)\\s*:?\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
            Matcher orgMatcher = orgPattern.matcher(userInput);
            if (orgMatcher.find()) {
                info.put("organization", orgMatcher.group(1).trim());
            }
            
        } catch (Exception e) {
            log.warn("Error extracting participant info: {}", e.getMessage());
        }
        
        return info;
    }
    
    private String getStr(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return null;
    }
    private LocalDateTime getTime(Map<String, Object> m, String... keys) {
        String s = getStr(m, keys);
        return (s == null) ? null : tryParseDateTime(s);
    }
    private Long getLong(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) continue;

            if (v instanceof Number) {
                // Hỗ trợ Integer, Long, Double, BigDecimal...
                return ((Number) v).longValue();
            }
            if (v instanceof String s) {
                s = s.trim();
                if (s.isEmpty()) continue;
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignore) { /* thử key khác */ }
            }
        }
        return null; // không tìm thấy/không parse được
    }

    /**
     * Xử lý câu hỏi về thông tin vé từ database thực tế
     */
    private String handleTicketInfoQuery(String userInput, float[] userVector) {
        System.out.println("🎯 DEBUG: handleTicketInfoQuery called with: '" + userInput + "'");
        try {
            // ✅ BƯỚC 1: SỬ DỤNG SERVICE CHUYÊN DỤNG ĐỂ TÌM SỰ KIỆN
            // Gọi EventVectorSearchService để tìm sự kiện khớp nhất, chỉ cần 1 kết quả
            List<Event> foundEvents = eventVectorSearchService.searchEvents(userInput, 0, 1); // userId=0 vì chưa cần lọc

            if (foundEvents.isEmpty()) {
                System.out.println("❌ DEBUG: EventVectorSearchService found no matching events.");
                // Nếu không tìm thấy, hãy hỏi lại người dùng
                return "Tôi hiểu bạn muốn xem thông tin vé, nhưng tôi chưa nhận ra tên sự kiện. Bạn có thể cho tôi biết tên sự kiện cụ thể được không?";
            }

            // Lấy sự kiện và tên sự kiện từ kết quả tìm kiếm
            Event event = foundEvents.get(0);
            String eventName = event.getTitle();
            System.out.println("✅ DEBUG: Extracted event name via EventVectorSearchService: '" + eventName + "'");

            // ✅ BƯỚC 2: PHẦN CÒN LẠI GIỮ NGUYÊN
            // Lấy thông tin vé từ database
            List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(event.getId());
            if (ticketTypes.isEmpty()) {
                return "ℹ️ Sự kiện \"" + event.getTitle() + "\" hiện chưa có thông tin vé nào được mở bán.";
            }

            // Tạo response với thông tin thực tế từ database
            StringBuilder response = new StringBuilder();
            // Sử dụng Markdown để định dạng cho đẹp hơn
            response.append("🎫 **Thông tin vé cho sự kiện: ").append(event.getTitle()).append("**\n");
            response.append("------------------------------------\n");

            for (TicketType ticket : ticketTypes) {
                response.append("• **Loại vé:** ").append(ticket.getName()).append("\n");
                response.append("  - **Giá:** ").append(String.format("%,d", ticket.getFinalPrice())).append(" VNĐ\n"); // Định dạng số cho dễ đọc
                response.append("  - **Còn lại:** ").append(ticket.getAvailableQuantity()).append(" vé\n");
                if (ticket.getDescription() != null && !ticket.getDescription().trim().isEmpty()) {
                    response.append("  - *Mô tả:* ").append(ticket.getDescription()).append("\n");
                }
                response.append("\n");
            }

            response.append("💡 Để mua vé, bạn chỉ cần nói 'Mua vé ").append(event.getTitle()).append("' nhé!");

            return response.toString();

        } catch (Exception e) {
            log.error("❌ Error handling ticket info query: {}", e.getMessage(), e);
            return "❌ Đã có lỗi xảy ra khi tôi cố gắng lấy thông tin vé. Vui lòng thử lại sau.";
        }
    }

}
