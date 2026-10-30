package com.thphatts.clinicportal.modules.concept.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "concept_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_concept_answers_question_answer",
                columnNames = {"question_id", "answer_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConceptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Concept question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false)
    private Concept answer;
}
