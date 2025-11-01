package com.group02.openevent.mapper;

import com.group02.openevent.dto.request.TicketUpdateRequest;
import com.group02.openevent.model.ticket.TicketType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    TicketType toTicketType(TicketUpdateRequest request);
    @Mapping(target = "ticketTypeId", source = "ticketTypeId")
    TicketUpdateRequest toDto(TicketType entity);
}
