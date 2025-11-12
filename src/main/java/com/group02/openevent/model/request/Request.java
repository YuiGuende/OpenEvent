package com.group02.openevent.model.request;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne
    private User sender;

    @ManyToOne
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @Column(name = "target_url", length = 255)
    private String targetUrl;

    @ManyToOne
    @JoinColumn(name = "order_order_id")
    private Order order;

    private String message;

    private String fileURL;

    @Column(nullable = true)
    private String responseMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public Request() {
    }

    @Override
    public String toString() {
        return "Request{" +
                "requestId=" + requestId +
                ", sender=" + sender +
                ", receiver=" + receiver +
                ", type=" + type +
                ", event=" + event.getId() +
                ", order=" + order.getOrderId() +
                ", message='" + message + '\'' +
                ", responseMessage='" + responseMessage + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

}
