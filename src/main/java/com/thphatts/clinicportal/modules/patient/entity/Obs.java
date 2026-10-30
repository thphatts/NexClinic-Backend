package com.thphatts.clinicportal.modules.patient.entity;

import com.thphatts.clinicportal.modules.concept.entity.Concept;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "obs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Obs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Patient person;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Column(name = "value_numeric")
    private Double valueNumeric;

    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_coded_id")
    private Concept valueCoded;

    @Column(name = "value_datetime")
    private LocalDateTime valueDatetime;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;
}
