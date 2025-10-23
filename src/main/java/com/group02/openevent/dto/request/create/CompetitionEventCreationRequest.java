package com.group02.openevent.dto.request.create;

import com.group02.openevent.dto.request.update.EventUpdateRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class CompetitionEventCreationRequest extends EventCreationRequest {
    String competitionType;
    String rules;
    String prizePool;
}
