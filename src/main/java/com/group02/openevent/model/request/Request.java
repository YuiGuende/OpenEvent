package com.group02.openevent.model.request;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
public class Request {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;
    
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public Request() {}
    
    public Request(Long hostId, Long eventId, RequestType type) {
        this.hostId = hostId;
        this.eventId = eventId;
        this.type = type;
    }
    
    // Getters and Setters
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    
    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }
    
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
