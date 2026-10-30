package com.thphatts.clinicportal.modules.concept.entity;

import com.thphatts.clinicportal.modules.concept.enums.ConceptClass;
import com.thphatts.clinicportal.modules.concept.enums.DataType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "concepts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataType datatype;

    @Enumerated(EnumType.STRING)
    @Column(name = "concept_class", nullable = false, length = 20)
    private ConceptClass conceptClass;
}
