package com.group02.openevent.dto.request.update;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class CompetitionEventUpdateRequest extends EventUpdateRequest {
    String competitionType;
    String rules;
    String prizePool;
}
