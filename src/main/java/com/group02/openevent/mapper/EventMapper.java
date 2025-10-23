package com.group02.openevent.mapper;

import com.group02.openevent.dto.request.*;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.response.*;
import com.group02.openevent.model.event.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EventMapper {

    // ===================== CREATE =====================
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "speakers", ignore = true)
    @Mapping(target = "places", ignore = true)
    @Mapping(target = "subEvents", ignore = true)
    @Mapping(target = "eventImages", ignore = true)
    @Mapping(target = "parentEvent", ignore = true)
    Event toEvent(EventCreationRequest request);

    // ===================== RESPONSE =====================
    @SubclassMapping(source = FestivalEvent.class, target = FestivalResponse.class)
    @SubclassMapping(source = WorkshopEvent.class, target = WorkshopResponse.class)
    @SubclassMapping(source = CompetitionEvent.class, target = CompetitionResponse.class)
    @SubclassMapping(source = MusicEvent.class, target = MusicResponse.class)
    @SubclassMapping(source = OtherEvent.class, target = OtherResponse.class)
    EventResponse toEventResponse(Event event);



    // ===================== UPDATE =====================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true) // Không được override ID
    @Mapping(target = "schedules", ignore = true) // Xử lý thủ công trong service
    @Mapping(target = "ticketTypes", ignore = true) // Xử lý thủ công trong service
    @Mapping(target = "eventImages", ignore = true) // Set thủ công để tránh orphan
    @Mapping(target = "subEvents", ignore = true) // Set thủ công để tránh orphan
    @Mapping(target = "speakers", ignore = true) // Set thủ công để tránh orphan
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "host", ignore = true)
    @Mapping(target = "parentEvent", ignore = true)
    @Mapping(target = "eventType", ignore = true)
    void updateEventFromRequest(EventUpdateRequest request, @MappingTarget Event event);



    @Mapping(target = "id", ignore = true) // Luôn bỏ qua ID khi mapping từ Request vào Entity mới
    @Mapping(target = "version", ignore = true)
    void createEventFromRequest(EventCreationRequest request, @MappingTarget Event event);


    // ===================== TO UPDATE REQUEST =====================
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "speakers", ignore = true)
    @Mapping(target = "places", ignore = true)
    @Mapping(target = "eventImages", ignore = true)
    EventUpdateRequest toUpdateRequest(Event event);


    // ===================== UTILITIES =====================
//    default Event toParent(Long parentEventId) {
//        if (parentEventId == null) return null;
//        Event parent = new Event();
//        parent.setId(parentEventId);
//        return parent;
//    }

    Speaker toSpeaker(SpeakerRequest dto);
}