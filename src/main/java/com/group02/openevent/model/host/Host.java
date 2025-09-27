package com.group02.openevent.model.host;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "host")
public class Host {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "host_id")
    private Long hostId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false,
		foreignKey = @ForeignKey(name = "fk_host_user"))
	private User user;

	@ManyToOne(optional = false)
	@JoinColumn(name = "event_id", nullable = false,
		foreignKey = @ForeignKey(name = "fk_host_event"))
	private Event event;

	@Column(name = "organize_id")
    private Long organizeId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public Long getHostId() {
		return hostId;
	}

	public void setHostId(Long hostId) {
		this.hostId = hostId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Long getOrganizeId() {
		return organizeId;
	}

	public void setOrganizeId(Long organizeId) {
		this.organizeId = organizeId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
