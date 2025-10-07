package com.group02.openevent.dto.response;

import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.event.Speaker;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class EventResponse {
    Long id;
    String title;
    String imageUrl;
    @Enumerated(EnumType.STRING)
    EventType eventType;
    String description;
    LocalDateTime publicDate;
    LocalDateTime enrollDeadline;
    LocalDateTime startsAt;
    LocalDateTime endsAt;
    LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    EventStatus status;
    String benefits;
    String learningObjects;
    Integer points;

    //     Integer parentEventId;
//     List<SubEventResponse> subEvents;
    List<EventSchedule> schedules;
    List<Speaker> speakers;
    List<Place> places;

}
