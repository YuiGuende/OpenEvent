package com.group02.openevent.ai.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.group02.openevent.dto.request.CompetitionEventCreationRequest;
import com.group02.openevent.dto.request.FestivalEventCreationRequest;
import com.group02.openevent.dto.request.MusicEventCreationRequest;
import com.group02.openevent.dto.request.WorkshopEventCreationRequest;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.OtherEvent;
import com.group02.openevent.model.event.Place;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MusicEventCreationRequest.class, name = "MUSIC"),
        @JsonSubTypes.Type(value = WorkshopEventCreationRequest.class, name = "WORKSHOP"),
        @JsonSubTypes.Type(value = FestivalEventCreationRequest.class, name = "FESTIVAL"),
        @JsonSubTypes.Type(value = CompetitionEventCreationRequest.class, name = "COMPETITION"),
        @JsonSubTypes.Type(value = OtherEvent.class, name = "OTHERS")
})
public class EventItem {

    private Long id;                // ID sự kiện
    private String title;              // event_title
    private String description;        // mô tả
    private LocalDateTime startsAt;    // thời gian bắt đầu
    private LocalDateTime endsAt;
    private List<Place> place;// thời gian kết thúc
    private LocalDateTime enrollDeadline; // hạn đăng ký
    private LocalDateTime createdAt;   // ngày tạo
    private EventType eventType;
    private EventStatus eventStatus;

}