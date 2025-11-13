package com.group02.openevent.mapper;

import com.group02.openevent.dto.request.*;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.response.*;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.ticket.TicketType;
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
    void createEventFromRequest(EventCreationRequest request, @MappingTarget Event event);


    // ===================== TO UPDATE REQUEST =====================
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "speakers", ignore = true)
    @Mapping(target = "places", ignore = true)
    @Mapping(target = "eventImages", ignore = true)
    EventUpdateRequest toUpdateRequest(Event event);
    
    // Default method to handle subclass-specific fields
    default EventUpdateRequest toUpdateRequestWithSubclassFields(Event event) {
        EventUpdateRequest request = toUpdateRequest(event);
        
        // Map subclass-specific fields based on event type
        if (event instanceof WorkshopEvent workshopEvent) {
            request.setTopic(workshopEvent.getTopic());
            request.setMaterialsLink(workshopEvent.getMaterialsLink());
            request.setMaxParticipants(workshopEvent.getMaxParticipants());
            request.setSkillLevel(workshopEvent.getSkillLevel());
            request.setPrerequisites(workshopEvent.getPrerequisites());
        } else if (event instanceof MusicEvent musicEvent) {
            request.setMusicType(musicEvent.getMusicType());
            request.setGenre(musicEvent.getGenre());
            request.setPerformerCount(musicEvent.getPerformerCount());
        } else if (event instanceof CompetitionEvent competitionEvent) {
            request.setCompetitionType(competitionEvent.getCompetitionType());
            request.setRules(competitionEvent.getRules());
            request.setPrizePool(competitionEvent.getPrizePool());
        } else if (event instanceof FestivalEvent festivalEvent) {
            request.setCulture(festivalEvent.getCulture());
            request.setHighlight(festivalEvent.getHighlight());
        }
        
        return request;
    }


    // ===================== UTILITIES =====================
//    default Event toParent(Long parentEventId) {
//        if (parentEventId == null) return null;
//        Event parent = new Event();
//        parent.setId(parentEventId);
//        return parent;
//    }

    Speaker toSpeaker(SpeakerRequest dto);
}