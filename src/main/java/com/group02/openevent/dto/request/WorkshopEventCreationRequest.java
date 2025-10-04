package com.group02.openevent.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkshopEventCreationRequest extends EventCreationRequest {
    String topic;
    String materialsLink;
}
