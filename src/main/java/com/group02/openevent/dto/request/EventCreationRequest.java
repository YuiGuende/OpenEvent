package com.group02.openevent.dto.request;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Host;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
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



}
