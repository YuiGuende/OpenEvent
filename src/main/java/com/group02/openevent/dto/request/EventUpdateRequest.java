package com.group02.openevent.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.group02.openevent.dto.ticket.TicketTypeRequest;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.Place;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MusicEventUpdateRequest.class, name = "MUSIC"),
        @JsonSubTypes.Type(value = FestivalEventUpdateRequest.class, name = "FESTIVAL"),
        @JsonSubTypes.Type(value = CompetitionEventUpdateRequest.class, name = "COMPETITION"),
        @JsonSubTypes.Type(value = WorkshopEventUpdateRequest.class, name = "WORKSHOP"),
})
public abstract class EventUpdateRequest {

    Long id; // Bắt buộc khi update

    String title;
    String imageUrl;
    String description;
    Integer capacity;

    String eventType; // Dùng cho Jackson chọn subclass

    LocalDateTime publicDate;
    LocalDateTime enrollDeadline;
    LocalDateTime startsAt;
    LocalDateTime endsAt;

    EventStatus status;

    String benefits;
    String learningObjects;
    Integer points;

    Long organizationId;
    Long hostId;
    Long parentEventId;

    // ❗ Dùng DTO thay vì entity
    List<Long> subEventIds;
    List<TicketTypeRequest> ticketTypes;
    List<EventImage> eventImages;
    List<EventSchedule> schedules;
    List<SpeakerRequest> speakers;
    List<Place> places;
}
