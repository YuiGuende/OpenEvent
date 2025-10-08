package com.group02.openevent.model.organization;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organization")
public class Organization {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "org_id")
	private Long orgId;

	@Column(name = "org_name", length = 150, nullable = false)
	private String orgName;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "website", length = 200)
	private String website;

	@Column(name = "email", length = 100)
	private String email;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "address", length = 300)
	private String address;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "representative_id", referencedColumnName = "customer_id",
            foreignKey = @ForeignKey(name = "fk_org_customer"))
    private Customer representative;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

	public Long getOrgId() {
		return orgId;
	}

	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

    public Customer getRepresentative() {
        return representative;
    }

    public void setRepresentative(Customer representative) {
        this.representative = representative;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
}
