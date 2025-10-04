package com.group02.openevent.mapper;

import com.group02.openevent.dto.request.EventCreationRequest;
import com.group02.openevent.dto.response.*;
import com.group02.openevent.model.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    Event toEvent(EventCreationRequest request);

    @SubclassMapping(source = FestivalEvent.class, target = FestivalResponse.class)
    @SubclassMapping(source = WorkshopEvent.class, target = WorkshopResponse.class)
    @SubclassMapping(source = CompetitionEvent.class, target = CompetitionResponse.class)
    @SubclassMapping(source = MusicEvent.class, target = MusicResponse.class)
    @SubclassMapping(source = OtherEvent.class, target = OtherResponse.class)
    EventResponse toEventResponse(Event event);




}
