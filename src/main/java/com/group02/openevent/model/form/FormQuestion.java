package com.group02.openevent.model.form;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "form_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne
    @JoinColumn(name = "form_id", nullable = false)
    @JsonBackReference
    private EventForm eventForm;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "question_options", columnDefinition = "TEXT")
    private String questionOptions; // JSON string for SELECT, CHECKBOX, RADIO options

    @Column(name = "question_order")
    private Integer questionOrder = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "formQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<FormResponse> responses;

    public enum QuestionType {
        TEXT, EMAIL, PHONE, SELECT, CHECKBOX, RADIO, TEXTAREA
    }
}
