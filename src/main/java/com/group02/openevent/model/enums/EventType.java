package com.group02.openevent.model.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum EventType {
    MUSIC, WORKSHOP, COMPETITION, FESTIVAL, OTHERS
}
