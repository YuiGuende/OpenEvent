package com.group02.openevent.dto.request.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.group02.openevent.dto.request.*;
import com.group02.openevent.dto.ticket.TicketTypeRequest;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.Place;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

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
        property = "eventType",
        visible = true // ✅ Cho phép field này được set luôn vào object
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MusicEventUpdateRequest.class, name = "MUSIC"),
        @JsonSubTypes.Type(value = FestivalEventUpdateRequest.class, name = "FESTIVAL"),
        @JsonSubTypes.Type(value = CompetitionEventUpdateRequest.class, name = "COMPETITION"),
        @JsonSubTypes.Type(value = WorkshopEventUpdateRequest.class, name = "WORKSHOP"),
})
public class EventUpdateRequest {

    Long id; // Bắt buộc khi update

    String title;
    String imageUrl;
    String description;
    Integer capacity;

    EventType eventType; // Dùng cho Jackson chọn subclass

    LocalDateTime publicDate;
    LocalDateTime enrollDeadline;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime startsAt;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime endsAt;

    EventStatus status;

    String benefits;
    String learningObjects;
    Integer points;

    Long organizationId;
    Long hostId;
    Long parentEventId;


    String competitionType;
    String rules;
    String prizePool;

    String culture;
    String highlight;

    String musicType;
    String genre;
    Integer performerCount;

    String topic;
    String materialsLink;
    Integer maxParticipants;
    String skillLevel;
    String prerequisites;

    // ❗ Dùng DTO thay vì entity
    List<Long> subEventIds;
    List<TicketTypeRequest> ticketTypes;
    List<EventImage> eventImages;
    List<EventSchedule> schedules;
    List<SpeakerRequest> speakers;
    List<Place> places;
}
