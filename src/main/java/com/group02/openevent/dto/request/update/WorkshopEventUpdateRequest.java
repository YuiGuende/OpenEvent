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
public class WorkshopEventUpdateRequest extends EventUpdateRequest {
    String topic;
    String materialsLink;
    Integer maxParticipants;
    String skillLevel;
    String prerequisites;
}
