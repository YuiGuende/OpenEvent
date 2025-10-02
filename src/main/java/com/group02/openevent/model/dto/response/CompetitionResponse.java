package com.group02.openevent.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompetitionResponse extends  EventResponse {
    String competitionType;
    String rules;
    String prizePool;
}
