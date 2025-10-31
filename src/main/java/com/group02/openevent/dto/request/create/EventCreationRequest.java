package com.group02.openevent.dto.request.create;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
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
public class EventCreationRequest {
    Integer id;
    String title;
    @Enumerated(EnumType.STRING)
    EventType eventType;

    String description;
    LocalDateTime publicDate;
    LocalDateTime enrollDeadline;
    LocalDateTime startsAt;
    LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    EventStatus status = EventStatus.DRAFT;

    Long hostId;
    Long organizationId;



}
