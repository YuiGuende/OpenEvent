package com.group02.openevent.ai.mapper;

import com.group02.openevent.ai.dto.EventItem;
import com.group02.openevent.model.event.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AIEventMapper {

    /**
     * Chuyển đổi từ DTO (EventItem) sang Entity (Event) để TẠO MỚI.
     */
    @Mapping(target = "status", source = "eventStatus") // Map tên khác nhau
    @Mapping(target = "places", source = "place")       // Map tên khác nhau
    @Mapping(target = "id", ignore = true)              // Luôn ignore id khi tạo mới
    @Mapping(target = "host", ignore = true)            // Sẽ được gán trong service
    @Mapping(target = "organization", ignore = true)  // Sẽ được gán trong service
    // Bỏ qua các quan hệ phức tạp, sẽ được xử lý ở tầng service
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "speakers", ignore = true)
    @Mapping(target = "subEvents", ignore = true)
    @Mapping(target = "eventImages", ignore = true)
    @Mapping(target = "parentEvent", ignore = true)
    @Mapping(target = "emailReminders", ignore = true)
    // Các trường này không có trong EventItem, để mặc định hoặc gán sau
    @Mapping(target = "poster", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "capacity", ignore = true)
    @Mapping(target = "publicDate", ignore = true)
    @Mapping(target = "benefits", ignore = true)
    @Mapping(target = "learningObjects", ignore = true)
    @Mapping(target = "points", ignore = true)
    Event toEvent(EventItem draft);

    /**
     * CẬP NHẬT một Entity (Event) đã tồn tại từ dữ liệu của DTO (EventItem).
     * Chỉ cập nhật các trường có giá trị non-null từ DTO.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", source = "eventStatus")
    @Mapping(target = "places", source = "place")
    // Không bao giờ cập nhật các trường quan trọng này từ DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "host", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "parentEvent", ignore = true)
    // Các quan hệ sẽ được quản lý riêng trong service
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "eventImages", ignore = true)
    @Mapping(target = "subEvents", ignore = true)
    @Mapping(target = "speakers", ignore = true)
    @Mapping(target = "emailReminders", ignore = true)
    void createEventFromRequest(EventItem draft, @MappingTarget Event event);

    /**
     * Chuyển đổi từ Entity (Event) sang DTO (EventItem) để xử lý phía AI hoặc trả về client.
     */
    @Mapping(target = "place", source = "places")
    @Mapping(target = "eventStatus", source = "status")
    EventItem toEventItem(Event event);
}
