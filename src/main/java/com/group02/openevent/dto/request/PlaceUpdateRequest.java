package com.group02.openevent.dto.request;

import com.group02.openevent.model.enums.Building;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceUpdateRequest {
    private Long id;
    private String placeName;
    private Building building;
    private Boolean isNew;
    private Boolean isDeleted;
}
