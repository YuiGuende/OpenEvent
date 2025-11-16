package com.group02.openevent.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.ai.dto.EventItem;
import com.group02.openevent.ai.dto.Message;
import com.group02.openevent.ai.dto.TimeSlot;
import com.group02.openevent.ai.mapper.AIEventMapper;
import com.group02.openevent.ai.model.Language;
import com.group02.openevent.ai.security.AISecurityService;
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
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.ai.ChatMessageRepo;
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
import java.util.concurrent.ConcurrentHashMap;
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
    private final EventVectorSearchService eventVectorSearchService;
    private final OrderAIService orderAIService;
    private final ICustomerRepo customerRepo;
    private final TicketTypeService ticketTypeService;
    private final AIEventMapper AIEventMapper;
    private final ChatMessageRepo chatMessageRepo;
    private final LanguageDetectionService languageDetectionService;
    private final TranslationService translationService;
    private final AISecurityService securityService;

    /**
     * PendingEvent neo theo sessionId để tránh đè nhau giữa nhiều phiên của cùng 1 user
     * Dùng ConcurrentHashMap để thread-safe trong bean singleton.
     */
    private final Map<String, PendingEvent> pendingEvents = new ConcurrentHashMap<>();

    public EventAIAgent(EmbeddingService embeddingService,
                        PlaceService placeService,
                        EventService eventService,
                        AgentEventService agentEventService,
                        VectorIntentClassifier classifier,
                        WeatherService weatherService,
                        LLM llm,
                        QdrantService qdrantService,
                        EventVectorSearchService eventVectorSearchService,
                        OrderAIService orderAIService,
                        ICustomerRepo customerRepo,
                        TicketTypeService ticketTypeService,
                        AIEventMapper AIEventMapper,
                        ChatMessageRepo chatMessageRepo,
                        LanguageDetectionService languageDetectionService,
                        TranslationService translationService,
                        AISecurityService securityService) {

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
        this.customerRepo = customerRepo;
        this.ticketTypeService = ticketTypeService;
        this.AIEventMapper = AIEventMapper;
        this.chatMessageRepo = chatMessageRepo;
        this.languageDetectionService = languageDetectionService;
        this.translationService = translationService;
        this.securityService = securityService;
        // KHÔNG giữ conversationHistory/pending theo user trong bean singleton
    }

    /* =========================
       SYSTEM PROMPT (cục bộ)
       ========================= */
    private String buildSystemPrompt() {
        StringBuilder systemPrompt = new StringBuilder();

        systemPrompt.append("""
Bạn là một Trợ lý AI giúp người dùng quản lý sự kiện và luôn nhớ các ngày lễ và sự kiện quan trọng của Việt Nam.
Hãy hiểu ngôn ngữ tự nhiên, linh hoạt với các mô tả như "tối nay", "cuối tuần", v.v.

## PHẠM VI HOẠT ĐỘNG:
Bạn là Trợ lý AI chuyên về hệ thống quản lý sự kiện OpenEvent. 
CHỈ trả lời các câu hỏi liên quan đến hệ thống OpenEvent:
✓ Sự kiện (Event) và quản lý sự kiện
✓ Mua vé, đặt vé, thanh toán (Ticket/Order/Payment)  
✓ Tìm kiếm sự kiện trên hệ thống
✓ Hướng dẫn sử dụng các tính năng của OpenEvent
✓ Thông tin về speakers, địa điểm, schedule của sự kiện
✓ Email reminders và thông báo
✓ Voucher và giảm giá
✓ Lịch trình và quản lý thời gian sự kiện
✓ Thời tiết (Weather) - để hỗ trợ lập kế hoạch sự kiện

KHÔNG trả lời các câu hỏi ngoài phạm vi:
✗ Lịch sử Việt Nam, thế giới (không liên quan sự kiện)
✗ Địa lý, khoa học chung (không liên quan sự kiện)
✗ Văn học, nghệ thuật (không liên quan sự kiện)
✗ Tin tức, chính trị (không liên quan sự kiện)
✗ Ẩm thực, thể thao (không liên quan sự kiện)
✗ Giải trí cá nhân không liên quan đến hệ thống
✗ Câu hỏi chung chung về bất kỳ chủ đề nào không thuộc OpenEvent

Khi nhận được câu hỏi ngoài phạm vi, hãy lịch sự từ chối và đề xuất hỗ trợ:

Ví dụ từ chối:
"Xin lỗi anh/chị, em chỉ có thể hỗ trợ về hệ thống OpenEvent và các sự kiện thôi ạ.
Em có thể giúp anh/chị:
- Tìm kiếm sự kiện
- Mua vé sự kiện
- Tạo và quản lý sự kiện
- Xem thông tin về speakers và địa điểm
Anh/chị cần hỗ trợ gì về OpenEvent ạ? 😊"

Nếu câu hỏi có liên quan đến sự kiện trên hệ thống OpenEvent (ví dụ: "Sự kiện âm nhạc", "Workshop Python", "Festival văn hóa"), thì TRẢ LỜI NGAY.

## VỀ CÁCH XƯNG HÔ VÀ GIAO TIẾP:
- Khi người dùng yêu cầu thay đổi cách xưng hô (ví dụ: "bạn là vợ tôi là chồng", "xưng hô anh em", "gọi tôi là em/chị/anh", "bạn là vợ"),
  hãy ghi nhớ và áp dụng ngay lập tức trong các câu trả lời tiếp theo.
- Ví dụ: 
  + Nếu người dùng nói "bạn là vợ tôi là chồng" → bạn sẽ xưng "em" và gọi người dùng là "anh".
  + Nếu người dùng nói "xưng hô anh em" → bạn sẽ xưng "em" và gọi người dùng là "anh" hoặc "chị".
  + Nếu người dùng nói "gọi tôi là em" → bạn sẽ gọi họ là "em" và xưng "chị/anh" tùy tình huống.
- Sau khi thay đổi cách xưng hô, tiếp tục sử dụng trong tất cả các câu trả lời sau đó.
- Không cần nhắc lại việc đã thay đổi cách xưng hô một cách dài dòng, chỉ cần xác nhận ngắn gọn và áp dụng ngay.
- Ví dụ: Người dùng nói "bạn là vợ tôi là chồng" → Bạn trả lời: "Dạ em hiểu rồi anh! Ồ, vậy anh cần em giúp gì hôm nay ạ?"

## MỤC TIÊU:
- Gợi ý, tạo, sửa, hoặc xoá sự kiện.
- Luôn phản hồi bằng văn bản tự nhiên (không hiện JSON).
- Nếu thiếu thông tin, hãy hỏi lại người dùng.
- Bạn hãy phản hồi dựa theo system message

## HƯỚNG DẪN TƯ VẤN NHƯ NHÂN VIÊN:
Khi người dùng hỏi về cách thao tác trên hệ thống hoặc cần hướng dẫn, hãy hướng dẫn chi tiết như một nhân viên tư vấn chuyên nghiệp:

### Hướng dẫn mua vé sự kiện:
- Nếu người dùng hỏi "Làm sao để mua vé?", "Mua vé như thế nào?", hãy hướng dẫn chi tiết:
  "Em hướng dẫn anh/chị mua vé như sau:
  1️⃣ Anh/chị cho em biết tên sự kiện muốn tham gia
  2️⃣ Em sẽ hiển thị tất cả loại vé có sẵn với giá và số lượng
  3️⃣ Anh/chị chọn loại vé phù hợp
  4️⃣ Cung cấp thông tin: tên, email, số điện thoại
  5️⃣ Em sẽ tóm tắt đơn hàng và anh/chị xác nhận
  6️⃣ Sau khi xác nhận, hệ thống sẽ tạo link thanh toán PayOS
  7️⃣ Anh/chị thanh toán và nhận vé qua email
  
  Anh/chị đã sẵn sàng rồi chứ? Vui lòng cho em biết sự kiện anh/chị muốn mua vé nhé! 😊"

### Hướng dẫn xem thông tin vé:
- Nếu người dùng hỏi "Xem vé ở đâu?", "Làm sao biết giá vé?", hãy hướng dẫn:
  "Anh/chị có thể hỏi em về thông tin vé của bất kỳ sự kiện nào.
  Ví dụ: 'Xem vé sự kiện X' hoặc 'Giá vé sự kiện Y là bao nhiêu?'
  Em sẽ hiển thị đầy đủ thông tin về các loại vé, giá cả và số lượng còn lại.
  
  Anh/chị muốn xem vé của sự kiện nào ạ? 🎫"

### Hướng dẫn tìm kiếm sự kiện:
- Nếu người dùng hỏi "Tìm sự kiện ở đâu?", hãy hướng dẫn:
  "Em có thể giúp anh/chị tìm sự kiện theo nhiều cách:
  - Theo tên sự kiện: 'Tìm sự kiện Music Night'
  - Theo loại sự kiện: 'Tìm workshop', 'Tìm concert'
  - Theo địa điểm: 'Tìm sự kiện ở Đà Nẵng'
  - Theo thời gian: 'Sự kiện hôm nay', 'Sự kiện tuần này'
  
  Anh/chị muốn tìm sự kiện như thế nào ạ? 🔍"

### Khi người dùng hỏi "sự kiện nổi bật / hot / gần đây":
- Hiểu là người dùng muốn xem MỘT VÀI sự kiện tiêu biểu, không phải toàn bộ danh sách.
- Ưu tiên:
  - Các sự kiện đang ở trạng thái PUBLIC
  - Có thời gian diễn ra gần ngày hiện tại
- Chỉ nên gợi ý khoảng 3–5 sự kiện, mỗi sự kiện nên có:
  - Tên sự kiện
  - Thời gian
  - Địa điểm (nếu có)
- Trả lời thân thiện, ví dụ:
  "Dạ, đây là một vài sự kiện nổi bật gần đây em gợi ý cho anh/chị nè: ..."

### Hướng dẫn tạo sự kiện:
- Nếu người dùng hỏi "Tạo sự kiện như thế nào?", hãy hướng dẫn:
  "Để tạo sự kiện, anh/chị cần cung cấp cho em các thông tin sau:
  1️⃣ Tên sự kiện
  2️⃣ Thời gian bắt đầu và kết thúc
  3️⃣ Địa điểm tổ chức
  4️⃣ Mô tả sự kiện (tùy chọn)
  5️⃣ Loại sự kiện (WORKSHOP, MUSIC, v.v.)
  
  Anh/chị có thể nói với em như: 'Tạo sự kiện Music Night vào 20h ngày 15/12 tại Nhà văn hóa'
  
  Em sẵn sàng hỗ trợ anh/chị tạo sự kiện ngay! 🎉"

### Luôn chủ động và thân thiện:
- Hỏi thêm thông tin nếu cần: "Để em hỗ trợ tốt hơn, anh/chị có thể cho em biết thêm [chi tiết cần thiết] không ạ?"
- Đề xuất giải pháp: "Em nghĩ anh/chị có thể thử [gợi ý] xem sao ạ!"
- Xác nhận lại để tránh hiểu nhầm: "Vậy là anh/chị muốn [tóm tắt lại yêu cầu] đúng không ạ?"
- Luôn sẵn sàng hỗ trợ: "Nếu có thắc mắc gì, anh/chị cứ hỏi em nhé! 😊"

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
         "capacity": 100
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

## NGUYÊN TẮC:
- Tránh dùng từ kỹ thuật với người dùng.
- Luôn đảm bảo rằng các trường TÊN SỰ KIỆN, THỜI GIAN BẮT ĐẦU/KẾT THÚC, và ĐỊA ĐIỂM đều được xác định. Nếu bất kỳ trường nào bị thiếu, hãy hỏi lại người dùng.
- Nếu phát hiện địa điểm và thời gian bị trùng với sự kiện khác, hãy hỏi lại người dùng một thời gian khác hoặc một địa điểm khác. Không tự ý thêm nếu bị trùng.
- Luôn diễn giải ý định rõ ràng, thân thiện.

## NGUYÊN TẮC DỮ LIỆU (RẤT QUAN TRỌNG):
- KHÔNG được tự bịa ra sự kiện, loại vé, giá vé, số lượng vé hoặc trạng thái sự kiện.
- Chỉ mô tả dựa trên dữ liệu mà hệ thống backend cung cấp (Event, TicketType, Order,...).
- Nếu hệ thống không trả về vé nào:
  → hãy trả lời kiểu: "Hiện tại sự kiện này chưa có thông tin vé được mở bán"
  hoặc "Em chưa thấy loại vé nào cho sự kiện này trong hệ thống".
- Nếu không chắc chắn 100% về thông tin, hãy hỏi lại người dùng hoặc đề nghị họ chọn từ danh sách sự kiện / vé mà hệ thống hiển thị.
- Luôn ưu tiên phản hồi an toàn thay vì đoán hoặc bịa thêm thông tin.
""");
        systemPrompt.append("- Ngày hiện tại là ")
                .append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(".\n");
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
   - Trích xuất tên sự kiện (nếu có) và thời gian nhắc nhở.
   [
      {
        "toolName": "SET_REMINDER",
        "args": {
          "event_title": "sự kiện ABC",
          "remind_minutes": 15
        }
      }
   ]
   - Không giải thích hay hiển thị nội dung JSON cho người dùng.
   - Lưu yêu cầu nhắc nhở vào hệ thống
   - Xác nhận với người dùng về việc sẽ gửi email

2. Hệ thống sẽ tự động gửi email nhắc nhở trước thời gian sự kiện bắt đầu
   - Email sẽ được gửi đến địa chỉ email của người dùng
   - Nội dung email bao gồm thông tin sự kiện và thời gian
""");
        return systemPrompt.toString();
    }

    /* =========================
       Xây context theo session
       ========================= */
    private List<Message> buildConversationContext(String sessionId, Long userId) {
        List<Message> ctx = new ArrayList<>();
        ctx.add(new Message("system", buildSystemPrompt()));

        if (sessionId == null) return ctx;

        try {
            List<com.group02.openevent.models.ai.ChatMessage> sessionMessages =
                    chatMessageRepo.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId);

            int startIndex = Math.max(0, sessionMessages.size() - 10); // lấy 10 tin gần nhất
            for (int i = startIndex; i < sessionMessages.size(); i++) {
                com.group02.openevent.models.ai.ChatMessage msg = sessionMessages.get(i);
                String role = Boolean.TRUE.equals(msg.getIsFromUser()) ? "user" : "assistant";
                ctx.add(new Message(role, msg.getMessage()));
            }
            log.debug("Loaded {} messages from session {} for user {}", sessionMessages.size(), sessionId, userId);
        } catch (Exception e) {
            log.warn("Failed to load session context for session {}: {}", sessionId, e.getMessage());
        }
        return ctx;
    }

    /* =========================
       Public APIs
       ========================= */

    /**
     * Xử lý input với context của phiên (multi-session).
     */
    @Transactional
    public String reply(String userInput, Long userId, String sessionId) throws Exception {
        List<Message> context = buildConversationContext(sessionId, userId);
        return processUserInput(userInput, userId, sessionId, context, null);
    }

    /**
     * Greeting không giữ state; dựng prompt tối thiểu.
     */
    public String getGreeting(String sessionId, Long userId) {
        List<Message> ctx = new ArrayList<>();
        ctx.add(new Message("system", buildSystemPrompt()));
        ctx.add(new Message("user", "Xin chào, tôi cần tư vấn quản lý lịch trình"));
        try {
            return llm.generateResponse(ctx);
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

    public String getConversationSummary(String sessionId, Long userId) {
        List<Message> ctx = buildConversationContext(sessionId, userId);
        if (ctx.size() <= 1) {
            return "Không có cuộc trò chuyện nào được ghi nhận.";
        }
        StringBuilder sb = new StringBuilder("📌 TÓM TẮT CUỘC TRÒ CHUYỆN:\n");
        for (Message msg : ctx) {
            if ("system".equals(msg.getRole())) continue;
            sb.append("user".equals(msg.getRole()) ? "🧑‍💻 Bạn: " : "🤖 AI: ")
                    .append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    public List<Event> getCurrentEvent(Long userId) {
        List<Event> events = eventService.getEventByUserId(userId);
        LocalDate today = LocalDate.now();

        return events.stream()
                .filter(e -> {
                    LocalDate start = e.getStartsAt().toLocalDate();
                    LocalDate end = e.getEndsAt().toLocalDate();
                    return !start.isAfter(today) && !end.isBefore(today);
                })
                .sorted(Comparator.comparing(Event::getStartsAt))
                .toList();
    }

    /* =========================
       Core processing
       ========================= */

    /**
     * Kiểm tra xem câu hỏi có ngoài phạm vi OpenEvent hay không
     */
    private boolean isOutOfScope(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }

        String input = userInput.toLowerCase();

        // Các từ khóa ngoài phạm vi
        String[] outOfScopeKeywords = {
                "lịch sử việt nam", "lịch sử trung quốc", "lịch sử mỹ", "tổng thống mỹ",
                "khoa học vật lý", "hóa học", "sinh học", "khoa học",
                "địa lý việt nam", "địa lý thế giới", "thủ đô của", "giới thiệu về",
                "văn học việt nam", "văn học thế giới", "nhà văn", "tác phẩm",
                "tin tức", "chính trị", "bầu cử", "quốc hội", "đảng chính trị",
                "nấu ăn", "món ăn", "công thức", "ẩm thực",
                "đá bóng", "world cup", "euro", "world series", "olympic",
                "giải trí", "phim ảnh", "mv", "nhạc mới", "game",
                "thời sự", "tin nóng", "sự kiện thế giới"
        };

        // Kiểm tra không chứa các từ khóa liên quan đến OpenEvent
        String[] openEventKeywords = {
                "sự kiện", "event", "vé", "ticket", "mua vé", "đặt vé",
                "workshop", "music", "festival", "competition", "conference",
                "speaker", "địa điểm", "location", "place",
                "thanh toán", "payment", "payos", "order",
                "reminder", "email", "thông báo", "nhắc nhở",
                "voucher", "giảm giá", "discount",
                "schedule", "lịch trình", "time",
                "thời tiết", "weather", "mưa", "nắng", "dự báo", "forecast"
        };

        // Nếu có từ khóa OpenEvent, không phải ngoài phạm vi
        for (String keyword : openEventKeywords) {
            if (input.contains(keyword)) {
                return false;
            }
        }

        // Kiểm tra nếu có từ khóa ngoài phạm vi
        for (String keyword : outOfScopeKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Phản hồi cho câu hỏi ngoài phạm vi
     */
    private String handleOutOfScopeQuestion() {
        return "Xin lỗi anh/chị, em chỉ có thể hỗ trợ về hệ thống OpenEvent và các sự kiện thôi ạ.\n\n" +
                "Em có thể giúp anh/chị:\n" +
                "✅ Tìm kiếm sự kiện\n" +
                "✅ Mua vé sự kiện\n" +
                "✅ Tạo và quản lý sự kiện\n" +
                "✅ Xem thông tin về speakers và địa điểm\n" +
                "✅ Thanh toán và voucher\n\n" +
                "Anh/chị cần hỗ trợ gì về OpenEvent ạ? 😊";
    }

    /**
     * Kiểm tra xem có phải câu hỏi về thời tiết không
     */
    private boolean isWeatherQuestion(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }

        String input = userInput.toLowerCase();

        String[] weatherKeywords = {
                "thời tiết", "weather", "mưa", "nắng", "dự báo", "forecast",
                "trời hôm nay", "thời tiết hôm nay", "ngày mai trời",
                "hôm nay trời", "weather today", "weather forecast"
        };

        for (String keyword : weatherKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Xử lý câu hỏi về thời tiết
     */
    private String handleWeatherQuestion(String userInput) {
        try {
            // Extract location from user input or use default
            String location = "Da Nang"; // Default location

            // Try to extract location from user input
            if (userInput.toLowerCase().contains("hà nội") || userInput.toLowerCase().contains("hanoi")) {
                location = "Ha Noi";
            } else if (userInput.toLowerCase().contains("hồ chí minh") ||
                    userInput.toLowerCase().contains("ho chi minh")) {
                location = "Ho Chi Minh City";
            } else if (userInput.toLowerCase().contains("đà nẵng") ||
                    userInput.toLowerCase().contains("da nang")) {
                location = "Da Nang";
            } else if (userInput.toLowerCase().contains("hải phòng")) {
                location = "Hai Phong";
            }

            // Get weather forecast for today
            LocalDateTime today = LocalDateTime.now();
            String forecastNote = weatherService.getForecastNote(today, location);

            if (forecastNote != null && !forecastNote.isEmpty()) {
                return "🌤 **Thời tiết:**\n" + forecastNote +
                        "\n\n💡 Lưu ý: Thời tiết có thể ảnh hưởng đến sự kiện ngoài trời. " +
                        "Anh/chị có thể cân nhắc khi lập kế hoạch sự kiện! 😊";
            } else {
                return "⚠️ Hiện tại em chưa thể lấy thông tin thời tiết chi tiết. " +
                        "Đề xuất anh/chị kiểm tra thời tiết trên ứng dụng thời tiết trước khi tổ chức sự kiện ngoài trời ạ! 😊";
            }
        } catch (Exception e) {
            log.error("Error getting weather forecast: {}", e.getMessage());
            return "⚠️ Xin lỗi, em không thể lấy thông tin thời tiết lúc này. " +
                    "Vui lòng thử lại sau hoặc kiểm tra thời tiết qua ứng dụng thời tiết ạ! 😊";
        }
    }

    @Transactional
    public String processUserInput(String userInput,
                                   Long userId,
                                   String sessionId,
                                   List<Message> context,
                                   HttpServletResponse response) throws Exception {


        // Kiểm tra câu hỏi có ngoài phạm vi không
        if (isOutOfScope(userInput)) {
            return handleOutOfScopeQuestion();
        }

        // Kiểm tra câu hỏi về thời tiết
        if (isWeatherQuestion(userInput)) {
            return handleWeatherQuestion(userInput);
        }

        boolean shouldReload = false;
        String redirectUrl = null; // NOTE: hiện tại chưa dùng; có thể tích hợp front-end nếu cần
        StringBuilder systemResult = new StringBuilder();

        /* ===== Pending theo SESSION ===== */
        if (sessionId != null && pendingEvents.containsKey(sessionId)) {
            String answer = userInput.trim().toLowerCase();
            if (answer.contains("có") || answer.contains("ok") || answer.contains("tiếp tục")) {
                EventItem pendingItem = pendingEvents.remove(sessionId).getEventItem();
                Event eventToSave = AIEventMapper.toEvent(pendingItem);
                agentEventService.saveEvent(eventToSave);
                return "📅 Đã tạo sự kiện: " + pendingItem.getTitle();
            } else if (answer.contains("không")) {
                pendingEvents.remove(sessionId);
                return "❌ Đã hủy tạo sự kiện do bạn từ chối.";
            } else {
                return "❓Bạn có thể xác nhận lại: có/không?";
            }
        }

        // ✅ NEW: nếu đang có pending order và user đang trả lời tên sự kiện
        if (orderAIService.hasPendingOrder(userId) && looksLikeEventNameAnswer(userInput)) {
            String eventName = extractEventNameFromBuyTicketInput("mua vé " + userInput);
            if (eventName != null && !eventName.isBlank()) {
                try {
                    return orderAIService.startOrderCreation(userId, eventName.trim());
                } catch (Exception e) {
                    log.error("Error starting order from event-name-only input: {}", e.getMessage(), e);
                }
            }
        }

        // ✅ NEW: Nếu KHÔNG có pending order nhưng user chỉ nói tên sự kiện
        if (!orderAIService.hasPendingOrder(userId)) {
            String lowerInput = userInput.toLowerCase().trim();
            
            // Kiểm tra nếu input có vẻ như chỉ là tên sự kiện
            if (looksLikeEventNameAnswer(userInput) || 
                (lowerInput.split("\\s+").length <= 3 && !lowerInput.contains("mua") && !lowerInput.contains("vé"))) {
                
                String eventName = extractEventNameFromBuyTicketInput(userInput);
                if (eventName != null && !eventName.isBlank()) {
                    // Thử tìm sự kiện và trigger flow mua vé
                    Optional<Event> eventOpt = eventService.getFirstPublicEventByTitle(eventName.trim());
                    if (eventOpt.isPresent()) {
                        return orderAIService.startOrderCreation(userId, eventName.trim());
                    }
                    
                    // Fuzzy match
                    String searchName = eventName.trim().toLowerCase();
                    List<Event> matches = eventService.getAllEvents().stream()
                            .filter(e -> e.getStatus() == EventStatus.PUBLIC)
                            .filter(e -> e.getStartsAt() != null && e.getStartsAt().isAfter(LocalDateTime.now()))
                            .filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(searchName))
                            .toList();
                    if (!matches.isEmpty()) {
                        return orderAIService.startOrderCreation(userId, matches.get(0).getTitle());
                    }
                }
            }
        }

        float[] userVector;
        try {
            userVector = embeddingService.getEmbedding(userInput);
        } catch (IllegalStateException e) {
            // Embedding service không khả dụng, set userVector = null để classifier dùng fallback
            log.warn("Embedding service không khả dụng, sử dụng fallback intent classification: {}", e.getMessage());
            userVector = null;
        } catch (Exception e) {
            log.error("Lỗi khi tạo embedding: {}", e.getMessage(), e);
            userVector = null;
        }

        /* ==================== ORDER FLOW ==================== */
        ActionType intent = classifier.classifyIntent(userInput, userVector);
        if (intent == ActionType.BUY_TICKET) {
            // Bước 1: Trích xuất tên sự kiện từ userInput
            String extractedEventName = extractEventNameFromBuyTicketInput(userInput);

            // Bước 2: Thử tìm kiếm exact match trong database trước (nếu có tên được trích xuất)
            Optional<Event> exactMatch = Optional.empty();
            if (extractedEventName != null && !extractedEventName.trim().isEmpty()) {
                exactMatch = eventService.getFirstPublicEventByTitle(extractedEventName.trim());
                if (exactMatch.isPresent()) {
                    return orderAIService.startOrderCreation(userId, extractedEventName.trim());
                }
            }

            // Bước 3: Nếu không tìm thấy exact match, thử vector search
            List<Event> foundEvents = eventVectorSearchService.searchEvents(userInput, userId, 1);
            if (!foundEvents.isEmpty()) {
                String eventName = foundEvents.get(0).getTitle();
                Optional<Event> eventOpt = eventService.getFirstPublicEventByTitle(eventName.trim());
                if (eventOpt.isPresent()) {
                    return orderAIService.startOrderCreation(userId, eventName.trim());
                }
            }

            // Bước 4: Nếu vector search cũng không tìm thấy, thử tìm kiếm fuzzy match
            if (extractedEventName != null && !extractedEventName.trim().isEmpty()) {
                String searchName = extractedEventName.trim().toLowerCase();
                List<Event> allPublicEvents = eventService.getAllEvents().stream()
                        .filter(e -> e.getStatus() == EventStatus.PUBLIC)
                        .filter(e -> e.getStartsAt() != null && e.getStartsAt().isAfter(LocalDateTime.now()))
                        .filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(searchName))
                        .toList();
                if (!allPublicEvents.isEmpty()) {
                    return orderAIService.startOrderCreation(userId, allPublicEvents.get(0).getTitle());
                }
            }

            // Bước 5: Nếu vẫn không tìm thấy, trả về thông báo lỗi
            return "Tôi hiểu bạn muốn mua vé, nhưng tôi chưa nhận ra tên sự kiện. Bạn có thể cho tôi biết tên sự kiện cụ thể được không?";
        }

        if (orderAIService.hasPendingOrder(userId)) {
            com.group02.openevent.ai.dto.PendingOrder pendingOrder = orderAIService.getPendingOrder(userId);

            switch (pendingOrder.getCurrentStep()) {
                case SELECT_EVENT -> {
                    // Khi tới đây mà chưa bắt được tên sự kiện bằng rule ở trên
                    return "ℹ️ Vui lòng cho biết tên sự kiện bạn muốn mua vé.";
                }
                case SELECT_TICKET_TYPE -> {
                    return orderAIService.selectTicketType(userId, userInput);
                }
                case PROVIDE_INFO -> {
                    Map<String, String> info = extractParticipantInfo(userInput);
                    return orderAIService.provideInfo(userId, info);
                }
                case CONFIRM_ORDER -> {
                    ActionType confirmIntent = classifier.classifyConfirmIntent(userInput, userVector);
                    switch (confirmIntent) {
                        case CONFIRM_ORDER -> {
                            Map<String, Object> result = orderAIService.confirmOrder(userId);
                            return String.valueOf(result.get("message"));
                        }
                        case CANCEL_ORDER -> {
                            return orderAIService.cancelOrder(userId);
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
        /* ==================== END ORDER FLOW ==================== */

        // Thêm user message vào context trước khi gọi LLM
        context.add(new Message("user", userInput));

        // Truy vấn LLM
        String aiResponse = llm.generateResponse(context);
        aiResponse = aiResponse
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "");

        // Tách JSON action (nếu có)
        Pattern jsonPattern = Pattern.compile("(\\[\\s*\\{[\\s\\S]*?\\}\\s*\\])");
        Matcher matcher = jsonPattern.matcher(aiResponse);
        String jsonPart = matcher.find() ? matcher.group() : null;

        // Text cho người dùng
        String userVisibleText = (jsonPart != null)
                ? aiResponse.replace(jsonPart, "").trim()
                : aiResponse.trim();

        // Parse actions
        List<Action> actions = tryParseActions(jsonPart);

        if (actions != null && !actions.isEmpty()) {
            for (Action action : actions) {
                String tool = action.getToolName();
                try {
                    switch (tool) {
                        case "ADD_EVENT" -> {
                            Map<String, Object> args = action.getArgs();

                            String title = getStr(args, "title", "event_title", "name");
                            LocalDateTime start = getTime(args, "start_time", "starts_at", "start", "from", "begin");
                            LocalDateTime end = getTime(args, "end_time", "ends_at", "end", "to", "finish");

                            if (title == null || start == null || end == null) {
                                systemResult.append("📝 Thiếu thông tin sự kiện (tiêu đề hoặc thời gian).\n");
                                break;
                            }
                            if (!start.isBefore(end)) {
                                systemResult.append("⛔ Thời gian không hợp lệ: bắt đầu phải trước kết thúc.\n");
                                break;
                            }

                            String placeName = getStr(args, "place", "location");
                            Optional<Place> placeOpt = Optional.empty();

                            if (placeName != null) {
                                try {
                                    float[] placeVec = embeddingService.getEmbedding(placeName);
                                    List<Map<String, Object>> searchResults = qdrantService.searchPlacesByVector(placeVec, 1);
                                    if (!searchResults.isEmpty()) {
                                        Object placeIdObj = searchResults.get(0).get("place_id");
                                        if (placeIdObj instanceof Number n) {
                                            placeOpt = placeService.findPlaceById(n.longValue());
                                        } else {
                                            log.warn("Qdrant result missing/invalid place_id; fallback DB");
                                            placeOpt = placeService.findPlaceByNameFlexible(placeName);
                                        }
                                    } else {
                                        placeOpt = placeService.findPlaceByNameFlexible(placeName);
                                    }
                                } catch (IllegalStateException e) {
                                    log.warn("Embedding service không khả dụng, dùng tìm kiếm place bằng tên: {}", e.getMessage());
                                    placeOpt = placeService.findPlaceByNameFlexible(placeName);
                                } catch (Exception e) {
                                    log.error("Qdrant Place search failed: {}", e.getMessage());
                                    placeOpt = placeService.findPlaceByNameFlexible(placeName);
                                }
                            }

                            if (placeOpt.isPresent()) {
                                List<Event> conflicted = eventService.isTimeConflict(start, end, List.of(placeOpt.get()));
                                if (!conflicted.isEmpty()) {
                                    systemResult.append("⚠️ Sự kiện bị trùng thời gian/địa điểm với:\n");
                                    for (Event conflict : conflicted) {
                                        systemResult.append(" - ").append(conflict.getTitle())
                                                .append(" (").append(conflict.getStartsAt())
                                                .append(" - ").append(conflict.getEndsAt()).append(")\n");
                                    }
                                    break;
                                }
                            } else {
                                String errorMsg = "⛔ Để tạo sự kiện, bạn cần cung cấp địa điểm hợp lệ.";
                                String placeNameRaw = getStr(args, "place", "location");
                                if (placeNameRaw != null && !placeNameRaw.isBlank()) {
                                    errorMsg += " Không tìm thấy địa điểm \"" + placeNameRaw + "\".";
                                } else {
                                    errorMsg += " Vui lòng cung cấp tên địa điểm.";
                                }
                                return errorMsg;
                            }

                            EventItem event = new EventItem();
                            event.setTitle(title);
                            event.setStartsAt(start);
                            event.setEndsAt(end);
                            event.setCreatedAt(LocalDateTime.now());

                            LocalDateTime defaultDeadline = start.minusHours(1);
                            LocalDateTime deadline = getTime(args, "enroll_deadline", "deadline");
                            event.setEnrollDeadline((deadline != null && deadline.isBefore(start)) ? deadline : defaultDeadline);

                            event.setEventStatus(EventStatus.DRAFT);

                            String eventTypeString = getStr(args, "event_type");
                            if (eventTypeString != null) {
                                try {
                                    event.setEventType(EventType.valueOf(eventTypeString.toUpperCase()));
                                } catch (IllegalArgumentException e) {
                                    event.setEventType(EventType.OTHERS);
                                }
                            } else {
                                event.setEventType(EventType.OTHERS);
                            }

                            if (args.containsKey("description")) {
                                event.setDescription((String) args.get("description"));
                            }

                            placeOpt.ifPresent(p -> event.setPlace(List.of(p)));

                            String intentWeather = classifier.classifyWeather(userInput, userVector);
                            if ("outdoor_activities".equals(intentWeather)) {
                                String forecastNote = weatherService.getForecastNote(start, "Da Nang");
                                if (forecastNote != null && !forecastNote.isEmpty()) {
                                    if (sessionId != null) pendingEvents.put(sessionId, new PendingEvent(event));
                                    return "🌦 " + forecastNote + "\n❓Bạn có muốn tiếp tục tạo sự kiện này không?";
                                }
                            }

                            Long orgId = getLong(args, "organization_id");

                            try {
                                log.info("Creating event: title={}, userId={}, orgId={}", event.getTitle(), userId, orgId);
                                Event saved = agentEventService.createEventByCustomer(userId, event, orgId);
                                systemResult.append("✅ Đã thêm sự kiện: ").append(saved.getTitle()).append("\n");
                                redirectUrl = "/events?create=true";
                            } catch (Exception e) {
                                log.error("Error creating event: {}", e.getMessage(), e);
                                systemResult.append("❌ Lỗi khi lưu sự kiện: ").append(e.getMessage()).append("\n");
                            }
                        }

                        case "UPDATE_EVENT" -> {
                            Map<String, Object> args = action.getArgs();
                            Event existing = null;

                            if (!args.containsKey("event_id") && !args.containsKey("original_title")) {
                                systemResult.append("❌ Thiếu định danh sự kiện. Hãy cung cấp `event_id` hoặc `original_title`.\n");
                                break;
                            }

                            if (args.containsKey("event_id")) {
                                Long eventId = getLong(args, "event_id");
                                if (eventId != null) {
                                    existing = eventService.getEventByEventId(eventId).orElse(null);
                                }
                            } else if (args.containsKey("original_title")) {
                                String oriTitle = getStr(args, "original_title");
                                existing = eventService.getFirstEventByTitle(oriTitle).orElse(null);
                            }

                            if (existing == null) {
                                systemResult.append("❌ Không tìm thấy sự kiện để cập nhật.\n");
                                break;
                            }

                            if (args.containsKey("title")) {
                                existing.setTitle(getStr(args, "title"));
                            }
                            if (args.containsKey("start_time") || args.containsKey("starts_at")) {
                                existing.setStartsAt(getTime(args, "start_time", "starts_at", "start", "from", "begin"));
                            }
                            if (args.containsKey("end_time") || args.containsKey("ends_at")) {
                                existing.setEndsAt(getTime(args, "end_time", "ends_at", "end", "to", "finish"));
                            }
                            if (args.containsKey("description")) {
                                existing.setDescription(getStr(args, "description"));
                            }
                            if (args.containsKey("image_url")) {
                                existing.setImageUrl(getStr(args, "image_url"));
                            }
                            if (args.containsKey("benefits")) {
                                existing.setBenefits(getStr(args, "benefits"));
                            }
                            if (args.containsKey("status")) {
                                try {
                                    existing.setStatus(EventStatus.valueOf(getStr(args, "status")));
                                } catch (IllegalArgumentException e) {
                                    systemResult.append("⚠️ Trạng thái không hợp lệ, giữ nguyên trạng thái cũ.\n");
                                }
                            }
                            if (args.containsKey("place")) {
                                String placeName = getStr(args, "place");
                                Place place = placeService.findPlaceByName(placeName).orElse(null);
                                if (place != null) {
                                    existing.setPlaces(List.of(place));
                                } else {
                                    systemResult.append("⚠️ Không tìm thấy địa điểm: ").append(placeName).append("\n");
                                }
                            }

                            eventService.saveEvent(existing);
                            systemResult.append("🔄 Đã cập nhật sự kiện: ").append(existing.getTitle()).append("\n");
                            shouldReload = true;
                        }

                        case "DELETE_EVENT" -> {
                            boolean deletedOne = false;
                            if (action.getArgs().containsKey("event_id")) {
                                Long id = getLong(action.getArgs(), "event_id");
                                if (id != null) {
                                    deletedOne = eventService.removeEvent(id);
                                }
                            } else if (action.getArgs().containsKey("title")) {
                                String title = getStr(action.getArgs(), "title");
                                deletedOne = eventService.deleteByTitle(title);
                            }

                            if (deletedOne) {
                                shouldReload = true;
                            } else {
                                systemResult.append("⚠️ Không tìm thấy sự kiện để xoá.\n");
                            }
                        }

                        case "SET_REMINDER" -> {
                            Map<String, Object> args = action.getArgs();
                            Long remindMinutes = getLong(args, "remind_minutes");

                            if (remindMinutes == null) {
                                systemResult.append("❓ Bạn muốn tôi nhắc trước bao nhiêu phút hoặc giờ?");
                                break;
                            }

                            String eventTitle = getStr(args, "event_title", "title");
                            Optional<Event> targetEventOpt = Optional.empty();

                            if (eventTitle != null && !eventTitle.isBlank()) {
                                List<Event> results = eventVectorSearchService.searchEvents(eventTitle, userId, 1);
                                if (!results.isEmpty()) {
                                    targetEventOpt = Optional.of(results.get(0));
                                }
                            }

                            if (targetEventOpt.isEmpty()) {
                                targetEventOpt = eventService.getNextUpcomingEventByUserId(userId);
                            }

                            if (targetEventOpt.isEmpty()) {
                                systemResult.append("❓ Mình không tìm thấy sự kiện nào để đặt lịch nhắc nhở.");
                                break;
                            }

                            Event finalEvent = targetEventOpt.get();

                            Optional<Customer> customerOpt = customerRepo.findByUser_Account_AccountId(userId);
                            if (customerOpt.isEmpty() || customerOpt.get().getUser() == null
                                    || customerOpt.get().getUser().getAccount() == null
                                    || customerOpt.get().getUser().getAccount().getEmail() == null) {
                                systemResult.append("❌ Tài khoản của bạn chưa có email để nhận thông báo.");
                                break;
                            }

                            String userEmail = customerOpt.get().getUser().getAccount().getEmail();
                            log.info("Saving reminder: eventId={}, userId={}, minutes={}", finalEvent.getId(), userId, remindMinutes);
                            agentEventService.createOrUpdateEmailReminder(finalEvent.getId(), remindMinutes.intValue(), userId);

                            systemResult.append("✅ Đã đặt lịch nhắc nhở cho sự kiện '")
                                    .append(finalEvent.getTitle())
                                    .append("' trước ").append(remindMinutes).append(" phút. Email sẽ được gửi đến ")
                                    .append(userEmail).append(".\n");
                        }
                    }
                } catch (Exception e) {
                    log.error("Action '{}' failed: {}", tool, e.getMessage(), e);
                    systemResult.append("❌ Lỗi khi xử lý hành động: ").append(tool).append("\n");
                }
            }
        } else {
            // Không có action JSON

            // --- BẮT ĐẦU SỬA LỖI HALLUCINATION ---
            ActionType fallbackIntent = classifier.classifyIntent(userInput, userVector);

            if (fallbackIntent == ActionType.PROMPT_SUMMARY_TIME ||
                    fallbackIntent == ActionType.QUERY_TICKET_INFO) {

                String realDataSummary;
                try {
                    if (fallbackIntent == ActionType.PROMPT_SUMMARY_TIME) {
                        realDataSummary = handleSummaryRequest(userInput, userId);
                    } else {
                        realDataSummary = handleTicketInfoQuery(userInput, userVector, userId);
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi chạy fallback intent: {}", e.getMessage());
                    return "❌ Đã có lỗi xảy ra khi tôi cố gắng tìm kiếm thông tin.";
                }

                if (realDataSummary == null ||
                        realDataSummary.startsWith("📭 Không có sự kiện") ||
                        realDataSummary.startsWith("ℹ️ Sự kiện") ||
                        realDataSummary.startsWith("📝 Mình không hiểu")) {

                    return "Dạ, hiện tại em chưa tìm thấy sự kiện nào phù hợp với yêu cầu của anh/chị ạ. Anh/chị có muốn em hỗ trợ tạo một sự kiện mới không? 😊";
                }

                return realDataSummary;
            }
            // --- KẾT THÚC SỬA LỖI ---

            // ✅ Chỉ dùng câu trả lời tự nhiên của LLM khi intent là UNKNOWN / ERROR
            if ((fallbackIntent == ActionType.UNKNOWN || fallbackIntent == ActionType.ERROR)
                    && !userVisibleText.isBlank()) {
                return userVisibleText;
            }

            // Các fallback intent còn lại (không phải tìm kiếm)
            switch (fallbackIntent) {
                case BUY_TICKET -> {
                    return "❌ Vui lòng bắt đầu lại quy trình mua vé bằng cách nói 'Mua vé [tên sự kiện]'";
                }
                case CONFIRM_ORDER, CANCEL_ORDER -> {
                    return "❌ Không có đơn hàng nào đang chờ xác nhận. Vui lòng bắt đầu quy trình mua vé trước.";
                }
                case PROMPT_FREE_TIME -> {
                    TimeContext timeContext = TimeSlotUnit.extractTimeContext(userInput);
                    String placeName = ""; // có thể trích từ userInput nếu cần
                    List<Event> busyEvents;

                    if (!placeName.isEmpty()) {
                        Place place = placeService.findPlaceByName(placeName).orElse(null);
                        if (place != null) {
                            busyEvents = eventService.getEventsByPlace(place.getId());
                        } else {
                            return "❌ Không tìm thấy địa điểm: " + placeName;
                        }
                    } else {
                        busyEvents = eventService.getAllEvents();
                    }

                    List<Event> filtered;
                    switch (timeContext) {
                        case TODAY -> filtered = TimeSlotUnit.filterEventsToday(busyEvents);
                        case TOMORROW -> filtered = TimeSlotUnit.filterEventsTomorrow(busyEvents);
                        case THIS_WEEK -> filtered = TimeSlotUnit.filterEventsThisWeek(busyEvents);
                        case NEXT_WEEK -> filtered = TimeSlotUnit.filterEventsNextWeek(busyEvents);
                        default -> filtered = busyEvents;
                    }

                    List<TimeSlot> freeSlots = TimeSlotUnit.findFreeTime(filtered);

                    StringBuilder sb = new StringBuilder("📆 Các khoảng thời gian rảnh");
                    if (!placeName.isEmpty()) sb.append(" tại ").append(placeName);
                    sb.append(":\n");

                    if (freeSlots.isEmpty()) {
                        sb.append("❌ Không có khoảng thời gian rảnh trong ").append(timeContext).append("\n");
                    } else {
                        for (TimeSlot slot : freeSlots) {
                            sb.append(" - ").append(slot.toString()).append("\n");
                        }
                    }
                    return sb.toString();
                }
                case PROMPT_SEND_EMAIL -> {
                    Pattern patternTime = Pattern.compile("trước (\\d{1,3}) ?(phút|giờ)");
                    Matcher matcherTime = patternTime.matcher(userInput.toLowerCase());

                    if (!matcherTime.find()) {
                        return "❓ Bạn muốn tôi nhắc trước bao nhiêu phút hoặc giờ? (Ví dụ: 'trước 45 phút').";
                    }

                    int value = Integer.parseInt(matcherTime.group(1));
                    String unit = matcherTime.group(2);
                    int remindMinutes = unit.equals("giờ") ? value * 60 : value;

                    String eventTitle = classifier.classifyEventTitle(userInput, userVector);
                    Optional<Event> targetEventOpt = Optional.empty();

                    if (eventTitle != null && !eventTitle.isBlank()) {
                        try {
                            List<Event> results = eventVectorSearchService.searchEvents(eventTitle, userId, 1);
                            if (!results.isEmpty()) targetEventOpt = Optional.of(results.get(0));
                        } catch (Exception e) {
                            log.error("Vector search failed for event title: {}", e.getMessage());
                        }
                    }

                    if (targetEventOpt.isEmpty()) {
                        targetEventOpt = eventService.getNextUpcomingEventByUserId(userId);
                        if (targetEventOpt.isEmpty()) {
                            return "❓ Mình không tìm thấy sự kiện cụ thể nào trong yêu cầu hoặc sự kiện sắp tới nào trong lịch của bạn.";
                        }
                    }

                    Event finalEvent = targetEventOpt.get();
                    Optional<Customer> customerOpt = customerRepo.findByUser_Account_AccountId(userId);
                    if (customerOpt.isEmpty() || customerOpt.get().getUser() == null
                            || customerOpt.get().getUser().getAccount() == null) {
                        return "❌ Không tìm thấy thông tin tài khoản của bạn. Vui lòng đăng nhập lại.";
                    }

                    String userEmail = customerOpt.get().getUser().getAccount().getEmail();
                    if (userEmail == null || userEmail.trim().isEmpty()) {
                        return "❌ Tài khoản của bạn chưa có địa chỉ email. Vui lòng cập nhật email trong thông tin cá nhân.";
                    }

                    agentEventService.createOrUpdateEmailReminder(finalEvent.getId(), remindMinutes, userId);
                    return "✅ Tôi đã cập nhật lịch nhắc nhở của bạn về sự kiện \"" + finalEvent.getTitle() +
                            "\" trước " + remindMinutes + " phút khi sự kiện bắt đầu.\n" +
                            "📧 Email sẽ được gửi đến: " + userEmail;
                }
                case UNKNOWN, ERROR -> {
                    return "❓ Tôi không hiểu yêu cầu của bạn. Bạn có thể thử hỏi về việc tạo sự kiện, xem sự kiện, hoặc nhắc bạn bằng gmail.";
                }
            }
        }

        // Nếu có cả userVisibleText và systemResult
        if (!systemResult.isEmpty()) {
            String fullResponse = (userVisibleText + "\n\n" + systemResult.toString().trim()).trim();
            if (shouldReload) fullResponse += "\n__RELOAD__";
            return fullResponse;
        }

        // Mặc định trả text tự nhiên
        return userVisibleText;
    }

    /* =========================
       Helpers
       ========================= */

    public static List<Action> tryParseActions(String jsonPart) {
        try {
            if (jsonPart == null || jsonPart.isEmpty()) {
                return Collections.emptyList();
            }
            ObjectMapper objectMapper = new ObjectMapper();
            List<Action> list = Arrays.asList(objectMapper.readValue(jsonPart, Action[].class));
            log.info("Parsed {} action(s).", list.size());
            return list;
        } catch (Exception e) {
            log.warn("Không thể parse Action(s): {}", e.getMessage());
            log.debug("JSON:\n{}", jsonPart);
            return Collections.emptyList();
        }
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
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("❌ Không thể parse ngày giờ: " + input);
    }

    public String handleSummaryRequest(String userInputs, Long userId) throws Exception {
        String lowerInput = userInputs.toLowerCase().trim();

        // Kiểm tra nếu là câu hỏi về "sự kiện nổi bật"
        if (lowerInput.contains("nổi bật") || lowerInput.contains("featured") ||
                lowerInput.contains("prominent") || lowerInput.contains("recommended")) {
            return handleFeaturedEventsRequest(userId);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        String range;

        if (userInputs.contains("hôm nay")) {
            start = now.toLocalDate().atStartOfDay();
            range = "hôm nay";
        } else if (userInputs.contains("ngày mai")) {
            start = now.plusDays(1).toLocalDate().atStartOfDay();
            range = "ngày mai";
        } else if (userInputs.contains("tuần này")) {
            DayOfWeek dow = now.getDayOfWeek();
            start = now.minusDays(dow.getValue() - 1).toLocalDate().atStartOfDay();
            range = "tuần này";
        } else if (userInputs.contains("tuần sau")) {
            DayOfWeek dow = now.getDayOfWeek();
            start = now.minusDays(dow.getValue() - 1).toLocalDate().atStartOfDay().plusWeeks(1);
            range = "tuần sau";
        } else {
            // Mặc định: tìm TẤT CẢ sự kiện sắp diễn ra
            start = now;
            range = "sắp diễn ra";
        }

        List<Event> allEvents = eventService.getAllEvents();

        List<Event> events = allEvents.stream()
                .filter(event -> event.getEndsAt().isAfter(start))
                .filter(event -> event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.CANCEL)
                .sorted(Comparator.comparing(Event::getStartsAt))
                .toList();

        if (events.isEmpty()) {
            return "📭 Không có sự kiện nào " + range + " ạ. Anh/chị có muốn em tìm các sự kiện khác không? 😊";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append("📆 **Các sự kiện ").append(range).append(":**\n\n");

        int index = 1;
        for (Event e : events) {
            sb.append("**").append(index).append(". ").append(e.getTitle()).append("**\n");
            sb.append("   🕒 **Thời gian:** ")
                    .append(e.getStartsAt().format(formatter))
                    .append(" - ")
                    .append(e.getEndsAt().format(formatter))
                    .append("\n");

            if (e.getPlaces() != null && !e.getPlaces().isEmpty()) {
                sb.append("   📍 **Địa điểm:** ").append(e.getPlaces().get(0).getPlaceName()).append("\n");
            }

            if (e.getEventType() != null) {
                sb.append("   🎭 **Loại:** ").append(e.getEventType()).append("\n");
            }

            sb.append("\n");
            index++;
        }

        sb.append("💡 Anh/chị muốn xem thông tin chi tiết hoặc mua vé cho sự kiện nào không ạ? 😊");
        return sb.toString();
    }

    /**
     * Xử lý câu hỏi về sự kiện nổi bật
     */
    private String handleFeaturedEventsRequest(Long userId) {
        try {
            List<com.group02.openevent.dto.home.EventCardDTO> posterEvents = eventService.getPosterEvents();
            List<com.group02.openevent.dto.home.EventCardDTO> featuredEvents = posterEvents;
            if (featuredEvents == null || featuredEvents.isEmpty()) {
                featuredEvents = eventService.getRecommendedEvents(10);
            }

            if (featuredEvents == null || featuredEvents.isEmpty()) {
                return "🌟 Hiện tại chưa có sự kiện nổi bật nào ạ. Anh/chị có muốn em tìm các sự kiện sắp diễn ra không? 😊";
            }

            LocalDateTime now = LocalDateTime.now();
            List<com.group02.openevent.dto.home.EventCardDTO> upcomingFeatured = featuredEvents.stream()
                    .filter(event -> {
                        if (event.getStartsAt() == null) return false;
                        return event.getStartsAt().isAfter(now) ||
                                (event.getEndsAt() != null && event.getEndsAt().isAfter(now));
                    })
                    .limit(10)
                    .toList();

            if (upcomingFeatured.isEmpty()) {
                return "🌟 Hiện tại chưa có sự kiện nổi bật nào sắp diễn ra ạ. Anh/chị có muốn em tìm các sự kiện khác không? 😊";
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            StringBuilder sb = new StringBuilder();
            sb.append("🌟 **Các sự kiện nổi bật gần đây:**\n\n");

            int index = 1;
            for (com.group02.openevent.dto.home.EventCardDTO event : upcomingFeatured) {
                sb.append("**").append(index).append(". ").append(event.getTitle()).append("**\n");

                if (event.getStartsAt() != null) {
                    sb.append("   🕒 **Thời gian:** ");
                    sb.append(event.getStartsAt().format(formatter));
                    if (event.getEndsAt() != null) {
                        sb.append(" - ").append(event.getEndsAt().format(formatter));
                    }
                    sb.append("\n");
                }

                if (event.getCity() != null && !event.getCity().trim().isEmpty() && !event.getCity().equals("TBA")) {
                    sb.append("   📍 **Địa điểm:** ").append(event.getCity()).append("\n");
                }

                if (event.getEventType() != null) {
                    sb.append("   🎭 **Loại:** ").append(event.getEventType()).append("\n");
                }

                sb.append("\n");
                index++;
            }

            sb.append("💡 Anh/chị muốn xem thông tin chi tiết hoặc mua vé cho sự kiện nào không ạ? 😊");

            return sb.toString();

        } catch (Exception e) {
            log.error("Error handling featured events request: {}", e.getMessage(), e);
            return "❌ Đã có lỗi xảy ra khi tôi cố gắng lấy danh sách sự kiện nổi bật. Vui lòng thử lại sau ạ.";
        }
    }

    private String extractEventName(String userInput) {
        String cleaned = userInput
                .replaceAll("(?i)(mua vé|mua ve|đăng ký|đăng ky|tham gia|đặt vé|dat ve|book vé|order vé|sự kiện|su kien|event)", "")
                .trim();
        return cleaned;
    }

    /**
     * Nhận diện các câu trả lời dạng "sự kiện đó tên là ..." để hiểu là user đang cung cấp tên sự kiện.
     */
    private boolean looksLikeEventNameAnswer(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().trim();
        if (lower.isEmpty()) return false;

        if (lower.startsWith("sự kiện đó tên là")
                || lower.startsWith("sự kiện tên là")
                || lower.startsWith("tên sự kiện là")
                || lower.startsWith("tên sự kiện")) {
            return true;
        }

        // Câu rất ngắn kiểu "sự kiện ku", "sự kiện abc"
        if (lower.startsWith("sự kiện") && lower.split("\\s+").length <= 5) {
            return true;
        }

        return false;
    }

    /**
     * Trích xuất tên sự kiện từ câu input khi người dùng muốn mua vé
     * Ví dụ: "tôi muốn mua vé sự kiện ku" -> "ku"
     */
    private String extractEventNameFromBuyTicketInput(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }

        String input = userInput.trim();

        // Pattern 1: "mua vé sự kiện [tên]" hoặc "mua vé [tên]"
        Pattern pattern1 = Pattern.compile(
                "(?i)(?:mua vé|mua ve|đăng ký|đăng ky|tham gia|đặt vé|dat ve|book vé|order vé)\\s+(?:sự kiện|su kien|event)?\\s*:?\\s*([^,]+?)(?:\\s|$|,|\\?|!)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher1 = pattern1.matcher(input);
        if (matcher1.find()) {
            String eventName = matcher1.group(1).trim();
            eventName = eventName.replaceAll("(?i)\\b(sự kiện|su kien|event|cho|về|tại)\\b", "").trim();
            if (!eventName.isEmpty() && eventName.length() >= 1) {
                return eventName;
            }
        }

        // Pattern 2: "sự kiện [tên]" ở cuối câu
        Pattern pattern2 = Pattern.compile(
                "(?i)sự kiện\\s+(?:tên)?\\s*:?\\s*([^,]+?)(?:\\s|$|,|\\?|!)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher2 = pattern2.matcher(input);
        if (matcher2.find()) {
            String eventName = matcher2.group(1).trim();
            if (!eventName.isEmpty() && eventName.length() >= 1) {
                return eventName;
            }
        }

        // Pattern 3: Tìm từ cuối cùng sau "vé" hoặc "sự kiện"
        Pattern pattern3 = Pattern.compile(
                "(?i)(?:vé|ve|sự kiện|su kien|event)\\s+([^\\s,]+?)(?:\\s|$|,|\\?|!)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher3 = pattern3.matcher(input);
        if (matcher3.find()) {
            String eventName = matcher3.group(1).trim();
            if (!eventName.isEmpty() && eventName.length() >= 1) {
                return eventName;
            }
        }

        // Pattern 4: Nếu không tìm thấy pattern nào, thử lấy từ cuối cùng (có thể là tên sự kiện ngắn)
        String[] words = input.split("\\s+");
        if (words.length > 0) {
            String lastWord = words[words.length - 1];
            lastWord = lastWord.replaceAll("[.,!?;:]", "").trim();
            if (!lastWord.isEmpty() && lastWord.length() >= 1 &&
                    !lastWord.equalsIgnoreCase("vé") &&
                    !lastWord.equalsIgnoreCase("ve") &&
                    !lastWord.equalsIgnoreCase("sự") &&
                    !lastWord.equalsIgnoreCase("kiện") &&
                    !lastWord.equalsIgnoreCase("event")) {
                return lastWord;
            }
        }

        return null;
    }

    private Map<String, String> extractParticipantInfo(String userInput) {
        Map<String, String> info = new HashMap<>();
        try {
            Pattern namePattern = Pattern.compile("(?:tên|ten|họ tên|ho ten|name)\\s*:?\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
            Matcher nameMatcher = namePattern.matcher(userInput);
            if (nameMatcher.find()) info.put("name", nameMatcher.group(1).trim());

            Pattern emailPattern = Pattern.compile("(?:email|mail|e-mail)\\s*:?\\s*([^,\\s]+@[^,\\s]+)", Pattern.CASE_INSENSITIVE);
            Matcher emailMatcher = emailPattern.matcher(userInput);
            if (emailMatcher.find()) info.put("email", emailMatcher.group(1).trim());

            Pattern phonePattern = Pattern.compile("(?:sđt|sdt|phone|số điện thoại|so dien thoai|điện thoại|dien thoai)\\s*:?\\s*([0-9]{9,11})", Pattern.CASE_INSENSITIVE);
            Matcher phoneMatcher = phonePattern.matcher(userInput);
            if (phoneMatcher.find()) info.put("phone", phoneMatcher.group(1).trim());

            Pattern orgPattern = Pattern.compile("(?:tổ chức|to chuc|organization|công ty|cong ty|trường|truong)\\s*:?\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
            Matcher orgMatcher = orgPattern.matcher(userInput);
            if (orgMatcher.find()) info.put("organization", orgMatcher.group(1).trim());

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

            if (v instanceof Number num) {
                return num.longValue();
            }
            if (v instanceof String s) {
                s = s.trim();
                if (s.isEmpty()) continue;
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignore) { }
            }
        }
        return null;
    }

    /**
     * Xử lý câu hỏi về thông tin vé từ database thực tế
     */
    private String handleTicketInfoQuery(String userInput, float[] userVector, Long userId) {
        log.debug("handleTicketInfoQuery called with: '{}'", userInput);
        try {
            String extractedEventName = extractEventNameFromBuyTicketInput(userInput);

            Optional<Event> exactMatch = Optional.empty();
            if (extractedEventName != null && !extractedEventName.trim().isEmpty()) {
                exactMatch = eventService.getFirstPublicEventByTitle(extractedEventName.trim());
                if (exactMatch.isPresent()) {
                    Event event = exactMatch.get();
                    return buildTicketInfoResponse(event);
                }
            }

            Long vectorUserId = (userId != null ? userId : 0L);
            List<Event> foundEvents = eventVectorSearchService.searchEvents(userInput, vectorUserId, 1);
            if (!foundEvents.isEmpty()) {
                Event event = foundEvents.get(0);
                return buildTicketInfoResponse(event);
            }

            if (extractedEventName != null && !extractedEventName.trim().isEmpty()) {
                String searchName = extractedEventName.trim().toLowerCase();
                List<Event> allPublicEvents = eventService.getAllEvents().stream()
                        .filter(e -> e.getStatus() == EventStatus.PUBLIC)
                        .filter(e -> e.getStartsAt() != null && e.getStartsAt().isAfter(LocalDateTime.now()))
                        .filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(searchName))
                        .toList();
                if (!allPublicEvents.isEmpty()) {
                    return buildTicketInfoResponse(allPublicEvents.get(0));
                }
            }

            return "Tôi hiểu bạn muốn xem thông tin vé, nhưng tôi chưa nhận ra tên sự kiện. Bạn có thể cho tôi biết tên sự kiện cụ thể được không?";

        } catch (Exception e) {
            log.error("Error handling ticket info query: {}", e.getMessage(), e);
            return "❌ Đã có lỗi xảy ra khi tôi cố gắng lấy thông tin vé. Vui lòng thử lại sau.";
        }
    }

    private String buildTicketInfoResponse(Event event) {
        List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(event.getId());
        if (ticketTypes.isEmpty()) {
            return "ℹ️ Sự kiện \"" + event.getTitle() + "\" hiện chưa có thông tin vé nào được mở bán.";
        }

        StringBuilder response = new StringBuilder();
        response.append("🎫 **Thông tin vé cho sự kiện: ").append(event.getTitle()).append("**\n");
        response.append("------------------------------------\n");

        for (TicketType ticket : ticketTypes) {
            response.append("• **Loại vé:** ").append(ticket.getName()).append("\n");
            response.append("  - **Giá:** ").append(String.format("%,d", ticket.getFinalPrice())).append(" VNĐ\n");
            response.append("  - **Còn lại:** ").append(ticket.getAvailableQuantity()).append(" vé\n");
            if (ticket.getDescription() != null && !ticket.getDescription().trim().isEmpty()) {
                response.append("  - *Mô tả:* ").append(ticket.getDescription()).append("\n");
            }
            response.append("\n");
        }

        response.append("💡 Để mua vé, bạn chỉ cần nói 'Mua vé ").append(event.getTitle()).append("' nhé!");
        return response.toString();
    }
}
