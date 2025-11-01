package com.group02.openevent.dto.notification;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventNotificationRequest {

    @NotNull(message = "Event ID không được để trống")
    private Long eventId;

    private String title;

    @NotBlank(message = "Nội dung thông báo không được để trống")
    private String message;

    @Override
    public String toString() {
        return "EventNotificationRequest{" +
                "eventId=" + eventId +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}