package com.group02.openevent.service;

import com.group02.openevent.model.event.*;

import java.util.Optional;

public interface EventService {
    MusicEvent saveMusicEvent(MusicEvent musicEvent);
    CompetitionEvent saveCompetitionEvent(CompetitionEvent competitionEvent);
    FestivalEvent saveFestivalEvent(FestivalEvent festivalEvent);
    WorkshopEvent saveWorkshopEvent(WorkshopEvent workshopEvent);
    Optional<Event> getEventById(Integer id);
}
