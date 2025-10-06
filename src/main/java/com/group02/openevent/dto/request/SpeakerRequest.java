package com.group02.openevent.dto.request;

import com.group02.openevent.model.enums.SpeakerRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerRequest {
    private String name;
    private String imageUrl;
    private String profile;
    private SpeakerRole defaultRole = SpeakerRole.SPEAKER; // hoặc Enum SpeakerRole nếu có
}
