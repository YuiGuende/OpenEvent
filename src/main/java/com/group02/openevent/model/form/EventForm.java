package com.group02.openevent.model.form;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.group02.openevent.model.event.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "event_forms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_id")
    private Long formId;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "form_title", nullable = false, length = 200)
    private String formTitle;

    @Column(name = "form_description", columnDefinition = "TEXT")
    private String formDescription;

    public enum FormType { REGISTER, CHECKIN, FEEDBACK, VOLUNTEER }

    @Enumerated(EnumType.STRING)
    @Column(name = "form_type", nullable = false, length = 20)
    private FormType formType = FormType.FEEDBACK;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "eventForm", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 30)
    @JsonManagedReference
    private List<FormQuestion> questions;

    @OneToMany(mappedBy = "eventForm", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 30)
    @JsonManagedReference
    private List<FormResponse> responses;
}
